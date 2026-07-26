#include "astra/security/security_engine.hpp"

namespace astra::security {

SecurityEngine::SecurityEngine(
    const capability::CapabilityMatrix& matrix
) : matrix_(matrix) {}

bool SecurityEngine::authorize(
    const PermissionRequest& request
) {
    /*
     * 1. 用户授权? (PermissionDatabase)
     */
    if (!database_.check(request.module_id, request.permission)) {
        return false;
    }

    /*
     * 2. Capability 支持? (CapabilityMatrix)
     */
    capability::Capability cap;
    if (permission_to_capability(request.permission, cap)) {
        if (!matrix_.has(cap)) {
            return false;
        }
    }

    /*
     * 3. Sandbox 允许? (SandboxEngine — future)
     */

    return true;
}

bool SecurityEngine::grant(
    const std::string& module,
    Permission permission
) {
    return database_.grant(module, permission);
}

bool SecurityEngine::revoke(
    const std::string& module,
    Permission permission
) {
    return database_.revoke(module, permission);
}

bool SecurityEngine::check(
    const std::string& module,
    Permission permission
) const {
    return database_.check(module, permission);
}

bool SecurityEngine::permission_to_capability(
    Permission p,
    capability::Capability& out
) {
    switch (p) {
        case Permission::ROOT_ACCESS:
            out = capability::Capability::ROOT_ACCESS;
            return true;
        case Permission::SYSTEM_WRITE:
            out = capability::Capability::SYSTEM_WRITE;
            return true;
        case Permission::MOUNT:
            out = capability::Capability::MOUNT_NAMESPACE;
            return true;
        case Permission::BOOT_PATCH:
            out = capability::Capability::BOOT_PATCH;
            return true;
        case Permission::SELINUX_CONTROL:
            out = capability::Capability::SELINUX_CONTROL;
            return true;
        case Permission::KERNEL_INTERFACE:
            out = capability::Capability::KERNEL_INTERFACE;
            return true;
        case Permission::NETWORK_ACCESS:
            out = capability::Capability::NETWORK;
            return true;
        case Permission::IPC_ACCESS:
            out = capability::Capability::IPC_ACCESS;
            return true;
        case Permission::FILESYSTEM_ACCESS:
            // No direct capability gate — filesystem access is governed by
            // the sandbox (Phase 8) rather than the provider capability
            // matrix. Treat as always-satisfiable at this layer.
            return false;
    }
    return false;
}

}  // namespace astra::security
