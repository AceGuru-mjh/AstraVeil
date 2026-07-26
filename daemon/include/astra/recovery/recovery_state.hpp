#pragma once

namespace astra::recovery {

/// Daemon recovery state machine.
enum class RecoveryState {
    HEALTHY,
    DEGRADED,
    RECOVERING,
    FAILED,
};

}  // namespace astra::recovery
