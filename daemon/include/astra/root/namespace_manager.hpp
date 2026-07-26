#pragma once

#include <sys/types.h>

namespace astra::root {

/// Creates and enters an isolated mount namespace for AstraRoot.
///
/// The model is: AstraRoot runs in its own mount namespace so that the
/// overlays it mounts (system / vendor / product) do not leak into the
/// init mount tree. Modules launched by AstraRoot inherit this
/// namespace, giving every module an isolated mount view without
/// per-module unshare() calls.
///
/// Phase 8 skeleton: [create] calls unshare(CLONE_NEWNS); [enter] and
/// [destroy] are stubs that become real when the namespace is held as a
/// long-lived child process (Phase 8.5).
class NamespaceManager {
public:
    NamespaceManager();
    ~NamespaceManager();

    /// Create a new mount namespace in the current process.
    /// Returns false (and leaves the tree untouched) on any kernel
    /// error or on non-Linux platforms.
    bool create();

    /// Re-enter the Astra namespace. No-op in Phase 8 (the namespace is
    /// the current process's); becomes a setns() in Phase 8.5.
    bool enter();

    /// Release the namespace. No-op in Phase 8.
    bool destroy();

    /// PID of the process owning the Astra namespace, or -1 before
    /// [create] / after [destroy].
    pid_t pid() const;

private:
    pid_t namespace_pid_ = -1;
};

}  // namespace astra::root
