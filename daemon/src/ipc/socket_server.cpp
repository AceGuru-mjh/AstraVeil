// ipc/socket_server.cpp
//
// Implementation of the AstraVeil daemon IPC server. Uses an AF_UNIX
// SOCK_STREAM socket with a 4-byte big-endian length prefix framing.
//
// Security: socket mode 0660 (owner+group only), SO_PEERCRED authentication
// on every connection, read timeout to prevent slowloris.

#include "astra/ipc/socket_server.hpp"

#include "astra/logger/logger.hpp"

#include <arpa/inet.h>
#include <fcntl.h>
#include <poll.h>
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
    if (g_server_for_signal != nullptr) {
        g_server_for_signal->stop();
    }
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
    ::signal(SIGPIPE, SIG_IGN);
}

// ---- P0-1 fix: peer authentication via SO_PEERCRED ----

bool isAllowedUid(uid_t uid) {
    constexpr uid_t UID_ROOT   = 0;
    constexpr uid_t UID_SYSTEM = 1000;
    constexpr uid_t UID_SHELL  = 2000;
    constexpr uid_t AID_APP_START = 10000;
    constexpr uid_t AID_APP_END   = 19999;

    return uid == UID_ROOT ||
           uid == UID_SYSTEM ||
           uid == UID_SHELL ||
           (uid >= AID_APP_START && uid <= AID_APP_END);
}

bool authenticatePeer(int fd) {
    struct ucred cred{};
    socklen_t len = sizeof(cred);
    if (::getsockopt(fd, SOL_SOCKET, SO_PEERCRED, &cred, &len) < 0) {
        ALOGE("socket_server: SO_PEERCRED failed: %s", std::strerror(errno));
        return false;
    }
    if (!isAllowedUid(cred.uid)) {
        ALOGE("socket_server: rejected connection from uid=%d pid=%d (not whitelisted)",
              cred.uid, cred.pid);
        return false;
    }
    ALOGI("socket_server: accepted peer uid=%d pid=%d", cred.uid, cred.pid);
    return true;
}

// ---- P1-15 fix: read with timeout to prevent slowloris ----

bool readExactWithTimeout(int fd, void* out, std::size_t n, int timeoutMs) {
    auto* p = static_cast<std::uint8_t*>(out);
    std::size_t got = 0;
    while (got < n) {
        struct pollfd pfd{};
        pfd.fd = fd;
        pfd.events = POLLIN;
        int pr = ::poll(&pfd, 1, timeoutMs);
        if (pr == 0) {
            ALOGW("socket_server: read timeout (%d ms)", timeoutMs);
            return false;
        }
        if (pr < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        ssize_t r = ::read(fd, p + got, n - got);
        if (r == 0) return false;
        if (r < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        got += static_cast<std::size_t>(r);
    }
    return true;
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

void SocketServer::set_handler(Handler handler) {
    handler_ = handler;
}

bool SocketServer::read_exact(int fd, void* out, std::size_t n) {
    return readExactWithTimeout(fd, out, n, 10000);  // 10s timeout
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

    // P0-1 fix: 0660 (owner+group only, no world access)
    ::chmod(socket_path_.c_str(), 0660);

    if (::listen(listen_fd_, 5) < 0) {
        ALOGE("socket_server: listen() failed: %s", std::strerror(errno));
        ::close(listen_fd_);
        listen_fd_ = -1;
        return false;
    }

    running_ = true;
    install_signal_handlers(this);
    ALOGI("socket_server: listening on %s (mode 0660, SO_PEERCRED auth)", socket_path_.c_str());
    return true;
}

void SocketServer::stop() {
    if (!running_.exchange(false)) return;
    if (listen_fd_ >= 0) {
        ::shutdown(listen_fd_, SHUT_RDWR);
        ::close(listen_fd_);
        listen_fd_ = -1;
    }
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

        // P0-1 fix: authenticate peer before handling
        if (!authenticatePeer(client_fd)) {
            ::close(client_fd);
            continue;
        }

        handle_client(client_fd);
        ::close(client_fd);
    }
}

void SocketServer::handle_client(int client_fd) {
    while (running_.load()) {
        std::uint32_t net_len = 0;
        if (!read_exact(client_fd, &net_len, sizeof(net_len))) {
            return;
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
