#pragma once

#include <atomic>

namespace astra {

/// Daemon lifecycle owner.
///
/// Phase 4: the daemon is a long-running service. [DaemonService] owns
/// the running flag and is the single place main() asks "should I keep
/// going?". Real service registration (init.rc / binder service) lands
/// in a later sub-phase; this is the lifecycle primitive.
class DaemonService {
public:
    DaemonService();

    /// Mark the service as started.
    bool start();

    /// Mark the service as stopped (asks the IPC loop to exit).
    void stop();

    /// True between [start] and [stop].
    bool isRunning();

private:
    std::atomic<bool> running_;
};

}  // namespace astra
