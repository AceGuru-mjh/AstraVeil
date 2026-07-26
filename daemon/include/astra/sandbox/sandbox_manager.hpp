#pragma once

#include <string>

namespace astra {

/// v3 per-module sandbox manager.
///
/// Drives the isolation lifecycle for a module process:
/// @code
/// ModuleRunner.launch(moduleId)
///   ↓
/// SandboxManager.create(moduleId)
///   ↓
/// (Phase 4) unshare(CLONE_NEWNS) + seccomp + landlock
///   ↓
/// module process runs confined
///   ↓
/// SandboxManager.destroy(moduleId)
/// @endcode
///
/// Phase 3.1 skeleton: [create]/[destroy] record intent. Real
/// namespace + seccomp + landlock enforcement lands in Phase 4.
class SandboxManager {
public:
    /// Create the sandbox for @p moduleId. Returns true on success.
    bool create(const std::string& moduleId);

    /// Destroy the sandbox for @p moduleId. Returns true on success.
    bool destroy(const std::string& moduleId);
};

}  // namespace astra
