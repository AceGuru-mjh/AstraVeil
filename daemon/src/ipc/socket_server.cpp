// ipc/socket_server.cpp
//
// Implementation of the AstraVeil daemon IPC server. Uses an AF_UNIX
// SOCK_STREAM socket with a 4-byte big-endian length prefix framing.
//
// Signal handling: SIGTERM and SIGINT call `stop()` via a small trampoline
// that only touches an atomic flag, which is async-signal-safe.

#include "astra/ipc/socket_server.hpp"

#include "astra/logger/logger.hpp"

#include <arpa/inet.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <unistd.h>

#include <cerrno>
#include <cstring>
#include <filesystem>
#include <system_error>

namespace astra::ipc {

namespace fs = std::filesystem;

namespace {

SocketServer* g_server_for_signal = nullptr;

void signal_trampoline(int signum) {
    // Async-signal-safe: only flip atomics and call signal-safe functions.
    if (g_server_for_signal != nullptr) {
        g_server_for_signal->stop();
    }
    // Re-install default so a second Ctrl+C kills us hard.
    ::signal(signum, SIG_DFL);
}

void install_signal_handlers(SocketServer* server) {
    g_server_for_signal = server;
    struct sigaction sa{};
    sa.sa_handler = &signal_trampoline;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = 0;
    ::sigaction(SIGTERM, &sa, nullptr);
    ::sigaction(SIGINT, &sa, nullptr);
    // Ignore SIGPIPE — a disconnected client should not kill the daemon.
    ::signal(SIGPIPE, SIG_IGN);
}

}  // namespace

SocketServer::SocketServer(std::string socket_path)
    : socket_path_(std::move(socket_path)) {}

SocketServer::~SocketServer() {
    stop();
    if (g_server_for_signal == this) {
        g_server_for_signal = nullptr;
    }
}

bool SocketServer::read_exact(int fd, void* out, std::size_t n) {
    auto* p = static_cast<std::uint8_t*>(out);
    std::size_t got = 0;
    while (got < n) {
        ssize_t r = ::read(fd, p + got, n - got);
        if (r == 0) return false;       // EOF
        if (r < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        got += static_cast<std::size_t>(r);
    }
    return true;
}

bool SocketServer::write_exact(int fd, const void* in, std::size_t n) {
    auto* p = static_cast<const std::uint8_t*>(in);
    std::size_t sent = 0;
    while (sent < n) {
        ssize_t w = ::write(fd, p + sent, n - sent);
        if (w < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        sent += static_cast<std::size_t>(w);
    }
    return true;
}

bool SocketServer::start() {
    // Create the parent directory (e.g. /dev/astra) with reasonable perms.
    try {
        const fs::path parent = fs::path(socket_path_).parent_path();
        if (!parent.empty() && !fs::exists(parent)) {
            std::error_code ec;
            fs::create_directories(parent, ec);
            if (ec) {
                ALOGE("socket_server: create_directories(%s) failed: %s",
                      parent.string().c_str(), ec.message().c_str());
                return false;
            }
            ::chmod(parent.string().c_str(), 0755);
        }
    } catch (const std::exception& e) {
        ALOGE("socket_server: parent dir setup failed: %s", e.what());
        return false;
    }

    // Remove any stale socket file at our path.
    ::unlink(socket_path_.c_str());

    listen_fd_ = ::socket(AF_UNIX, SOCK_STREAM, 0);
    if (listen_fd_ < 0) {
        ALOGE("socket_server: socket() failed: %s", std::strerror(errno));
        return false;
    }

    sockaddr_un addr{};
    addr.sun_family = AF_UNIX;
    if (socket_path_.size() >= sizeof(addr.sun_path)) {
        ALOGE("socket_server: socket path too long: %s", socket_path_.c_str());
        ::close(listen_fd_);
        listen_fd_ = -1;
        return false;
    }
    std::strncpy(addr.sun_path, socket_path_.c_str(), sizeof(addr.sun_path) - 1);

    if (::bind(listen_fd_, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
        ALOGE("socket_server: bind(%s) failed: %s", socket_path_.c_str(), std::strerror(errno));
        ::close(listen_fd_);
        listen_fd_ = -1;
        return false;
    }

    ::chmod(socket_path_.c_str(), 0666);

    if (::listen(listen_fd_, 5) < 0) {
        ALOGE("socket_server: listen() failed: %s", std::strerror(errno));
        ::close(listen_fd_);
        listen_fd_ = -1;
        return false;
    }

    running_ = true;
    install_signal_handlers(this);
    ALOGI("socket_server: listening on %s", socket_path_.c_str());
    return true;
}

void SocketServer::stop() {
    if (!running_.exchange(false)) return;
    if (listen_fd_ >= 0) {
        ::shutdown(listen_fd_, SHUT_RDWR);
        ::close(listen_fd_);
        listen_fd_ = -1;
    }
    // Best-effort cleanup of the socket file.
    ::unlink(socket_path_.c_str());
    ALOGI("socket_server: stopped");
}

void SocketServer::run() {
    if (listen_fd_ < 0) {
        ALOGE("socket_server: run() called before start()");
        return;
    }
    while (running_.load()) {
        int client_fd = ::accept(listen_fd_, nullptr, nullptr);
        if (client_fd < 0) {
            if (errno == EINTR) continue;
            if (!running_.load()) break;
            ALOGW("socket_server: accept() failed: %s", std::strerror(errno));
            continue;
        }
        handle_client(client_fd);
        ::close(client_fd);
    }
}

void SocketServer::handle_client(int client_fd) {
    // Loop so a single connection can issue multiple pipelined requests.
    while (running_.load()) {
        std::uint32_t net_len = 0;
        if (!read_exact(client_fd, &net_len, sizeof(net_len))) {
            return;  // client closed or error
        }
        std::uint32_t len = ntohl(net_len);
        if (len == 0 || len > (8 * 1024 * 1024)) {
            ALOGW("socket_server: bad frame length %u", len);
            return;
        }
        std::vector<std::uint8_t> payload(len);
        if (!read_exact(client_fd, payload.data(), len)) {
            ALOGW("socket_server: short read on payload");
            return;
        }

        std::vector<std::uint8_t> response;
        if (handler_) {
            response = handler_(payload);
        } else {
            ALOGW("socket_server: no handler installed; dropping request");
            response.clear();
        }

        if (response.empty()) {
            // No response body — still send a 0-length frame so the client
            // knows the request was processed.
            std::uint32_t zero = htonl(0);
            write_exact(client_fd, &zero, sizeof(zero));
            return;
        }

        std::uint32_t net_resp_len = htonl(static_cast<std::uint32_t>(response.size()));
        if (!write_exact(client_fd, &net_resp_len, sizeof(net_resp_len)) ||
            !write_exact(client_fd, response.data(), response.size())) {
            ALOGW("socket_server: failed to write response");
            return;
        }
    }
}

}  // namespace astra::ipc
