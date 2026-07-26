#pragma once

#include <atomic>
#include <thread>

namespace astra {

/// Unix Domain Socket IPC server.
///
/// Listens on `/data/local/tmp/astrad.sock`, accepts one connection at a
/// time, reads a length-prefixed protobuf frame, and dispatches it to a
/// [RequestHandler]. Phase 2.1 skeleton: the loop accepts + closes;
/// Phase 2.1-B wires real protobuf decode.
class IpcServer {
public:
    IpcServer();
    ~IpcServer();

    /// Bind + listen + spawn the accept loop thread.
    bool start();

    /// Signal the loop to stop and join the worker thread.
    void stop();

private:
    void loop();

    int socketFd_;
    std::atomic<bool> running_;
    std::thread worker_;
};

}  // namespace astra
