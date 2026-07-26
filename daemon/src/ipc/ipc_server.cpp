#include "astra/ipc/ipc_server.hpp"

#include "astra/logger/logger.hpp"

#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <cstdint>
#include <cstring>
#include <vector>

namespace astra {

namespace {

constexpr char kSocketPath[] = "/data/local/tmp/astrad.sock";

}  // namespace

IpcServer::IpcServer()
    : socketFd_(-1), running_(false) {}

IpcServer::~IpcServer() {
    stop();
}

bool IpcServer::start() {
    socketFd_ = ::socket(AF_UNIX, SOCK_STREAM, 0);
    if (socketFd_ < 0) {
        ALOGE("IpcServer: socket() failed");
        return false;
    }

    sockaddr_un addr{};
    addr.sun_family = AF_UNIX;
    std::strncpy(addr.sun_path, kSocketPath, sizeof(addr.sun_path) - 1);
    ::unlink(kSocketPath);  // remove stale socket file

    if (::bind(socketFd_, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) {
        ALOGE("IpcServer: bind(%s) failed", kSocketPath);
        ::close(socketFd_);
        socketFd_ = -1;
        return false;
    }

    if (::listen(socketFd_, 8) < 0) {
        ALOGE("IpcServer: listen() failed");
        ::close(socketFd_);
        socketFd_ = -1;
        return false;
    }

    running_ = true;
    ALOGI("IpcServer: listening on %s", kSocketPath);
    worker_ = std::thread(&IpcServer::loop, this);
    return true;
}

void IpcServer::loop() {
    while (running_) {
        int client = ::accept(socketFd_, nullptr, nullptr);
        if (client < 0) {
            if (running_) {
                ALOGW("IpcServer: accept() failed");
            }
            continue;
        }

        // Read a length-prefixed frame: [4-byte big-endian length][payload].
        std::uint32_t length = 0;
        ssize_t n = ::read(client, &length, sizeof(length));
        if (n != sizeof(length)) {
            ::close(client);
            continue;
        }

        // Payload buffer (cap at 1 MiB to bound memory).
        if (length == 0 || length > (1u << 20)) {
            ::close(client);
            continue;
        }
        std::vector<char> buf(length);
        std::size_t got = 0;
        while (got < length) {
            n = ::read(client, buf.data() + got, length - got);
            if (n <= 0) break;
            got += static_cast<std::size_t>(n);
        }

        if (got == length) {
            // Dispatch — Phase 2.1-B wires the real RequestHandler.
            ALOGI("IpcServer: received %u-byte frame", length);
        }
        ::close(client);
    }
}

void IpcServer::stop() {
    running_ = false;
    if (socketFd_ >= 0) {
        ::close(socketFd_);
        socketFd_ = -1;
    }
    ::unlink(kSocketPath);
    if (worker_.joinable()) {
        worker_.join();
    }
}

}  // namespace astra
