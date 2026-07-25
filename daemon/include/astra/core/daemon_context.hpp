#pragma once

// astra/core/daemon_context.hpp
//
// Shared, mutable state for the running daemon. Passed by reference to
// subsystems that need to inspect or update it (e.g. `ProviderService`
// flips `provider_online` and writes `active_provider`).

#include <atomic>
#include <memory>
#include <string>

namespace astra::core {

struct DaemonContext {
    /// Where the Unix domain socket lives. Default matches the
    /// on-device path expected by the AstraVeil client library.
    std::string socket_path = "/dev/astra/astrad.sock";

    /// Daemon version string (mirrors `astra_rust::version()` initially).
    std::string version = "0.1.0";

    /// Set to true by `main()` while the accept loop should keep running.
    std::atomic<bool> running{false};

    /// Flipped by `ProviderService` once a root provider handshake succeeds.
    std::atomic<bool> provider_online{false};

    /// Human-readable name of the currently active provider ("magisk",
    /// "kernelsu", "apatch", "astraroot", or "none").
    std::string active_provider = "none";
};

}  // namespace astra::core
