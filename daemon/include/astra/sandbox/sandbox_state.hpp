#pragma once

namespace astra {

/// Lifecycle state of a per-module sandbox.
enum class SandboxState {
    CREATED,
    ISOLATED,
    RUNNING,
    FAILED,
};

}  // namespace astra
