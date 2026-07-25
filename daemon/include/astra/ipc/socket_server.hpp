#pragma once

// astra/ipc/socket_server.hpp
//
// Length-prefixed Unix domain socket server. The framing used today is:
//
//   [4 bytes big-endian length N][N bytes payload]
//
// where the first payload byte is a request-type discriminator (see
// `astra::RequestType` in `main.cpp`) and the remaining bytes are a
// UTF-8 JSON body. Responses use the same framing.
//
// This will be replaced by a protobuf-framed protocol once
// `proto/astra.proto` lands; see the migration note in
// `daemon/CMakeLists.txt`.

#include <atomic>
#include <cstdint>
#include <functional>
#include <string>
#include <vector>

namespace astra::ipc {

class SocketServer {
public:
    /// Handler signature: receives the raw request payload (including the
    /// request-type byte) and returns the raw response payload to send back.
    /// An empty vector means "no response — close the connection".
    using Handler = std::function<std::vector<uint8_t>(const std::vector<uint8_t>&)>;

    explicit SocketServer(std::string socket_path);
    ~SocketServer();

    SocketServer(const SocketServer&) = delete;
    SocketServer& operator=(const SocketServer&) = delete;

    /// Create the socket, bind it, and start listening. Returns false on
    /// failure (error is logged via `astra::logger`).
    bool start();

    /// Stop the server and unlink the socket file. Safe to call from a
    /// signal handler context (only flips an atomic flag).
    void stop();

    /// Blocking accept loop. Returns when `stop()` is called or a fatal
    /// accept error occurs.
    void run();

    /// Install the per-request handler. Must be called before `run()`.
    void set_handler(Handler handler);

    /// Returns the configured socket path.
    const std::string& socket_path() const noexcept { return socket_path_; }

private:
    std::string socket_path_;
    int listen_fd_{-1};
    std::atomic<bool> running_{false};
    Handler handler_;

    /// Read exactly `n` bytes from `fd` into `out`. Returns false on EOF
    /// or error.
    static bool read_exact(int fd, void* out, std::size_t n);

    /// Write exactly `n` bytes to `fd`. Returns false on error.
    static bool write_exact(int fd, const void* in, std::size_t n);

    /// Handle one already-accepted client connection.
    void handle_client(int client_fd);
};

}  // namespace astra::ipc
