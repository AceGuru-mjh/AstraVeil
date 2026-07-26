#pragma once

namespace astra::runtime {

/// Daemon lifecycle state.
///
///   STARTING → READY → RUNNING → STOPPING → (terminal)
///                              ↘ ERROR
enum class DaemonState {
    STARTING,
    READY,
    RUNNING,
    STOPPING,
    ERROR,
};

}  // namespace astra::runtime
