#pragma once

#include "astra/security/permission.hpp"
#include "astra/security/permission_database.hpp"
#include "astra/capability/capability_matrix.hpp"

namespace astra::security {

/// Central authorisation decision point for the AstraVeil security framework.
///
/// Decision tree (Phase 7 implements steps 1 + 2; step 3 is stubbed for
/// the sandbox engine):
///
/// @code
/// 模块请求 (PermissionRequest)
///       ↓
/// 1. 用户授权?          (PermissionDatabase)
///       ↓ no  → DENY "permission not granted"
/// 2. Capability 支持?  (CapabilityMatrix)
///       ↓ no  → DENY "capability unavailable"
/// 3. Sandbox 允许?      (SandboxEngine — future)
///       ↓ no  → DENY "sandbox denied"
///       ↓ yes → ALLOW
/// @endcode
///
/// The engine is constructed once by the daemon and shared with every
/// subsystem that needs to authorise an action (IPC layer, module
/// runtime, ...). It is safe to call from any thread — the underlying
/// [PermissionDatabase] is guarded internally.
class SecurityEngine {
public:
    /// @param matrix  The live device capability matrix. Must outlive the
    ///                engine. The engine re-reads it on every [authorize]
    ///                call so it always reflects the current provider.
    explicit SecurityEngine(
        const capability::CapabilityMatrix& matrix
    );

    /// Run the full decision tree for @p request.
    /// @return true iff the request is ALLOWED.
    bool authorize(
        const PermissionRequest& request
    );

    /// Grant @p permission to @p module (called after the user accepts
    /// the AstraUI permission dialog).
    bool grant(
        const std::string& module,
        Permission permission
    );

    /// Revoke @p permission from @p module.
    bool revoke(
        const std::string& module,
        Permission permission
    );

    /// @return true iff @p module has been granted @p permission.
    bool check(
        const std::string& module,
        Permission permission
    ) const;

private:
    /// Map a security [Permission] to the [capability::Capability] it
    /// requires. Returns false for permissions that have no capability
    /// equivalent (e.g. FILESYSTEM_ACCESS — always satisfiable).
    static bool permission_to_capability(
        Permission p,
        capability::Capability& out
    );

    PermissionDatabase database_;
    const capability::CapabilityMatrix& matrix_;
};

}  // namespace astra::security
