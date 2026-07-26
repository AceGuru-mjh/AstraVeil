#include "astra/module/module_permission_checker.hpp"

namespace astra::module {

namespace {

/// Resolve a permission name string to a Capability. Returns false if
/// the name does not match any known capability.
bool resolve_capability(
    const std::string& name,
    capability::Capability& out
) {
    for (const auto c : capability::all_capabilities()) {
        if (capability::capability_name(c) == name) {
            out = c;
            return true;
        }
    }
    return false;
}

}  // namespace

bool ModulePermissionChecker::check(
    const ModuleManifest& manifest,
    const capability::CapabilityMatrix& matrix
) {
    /*
     * 权限映射:
     *
     *   manifest.permissions[i].name   →   Capability (by name)
     *   ↓
     *   matrix.has(Capability)         →   ALLOW / DENY
     *
     * A permission name that does not match any Capability is treated
     * as a denial — modules may not request unknown capabilities.
     */
    for (const auto& permission : manifest.permissions) {
        capability::Capability resolved;
        if (!resolve_capability(permission.name, resolved)) {
            // Unknown permission — refuse.
            return false;
        }
        if (!matrix.has(resolved)) {
            // Capability exists in the vocabulary but the device does
            // not offer it (e.g. BOOT_PATCH on an unrooted device).
            return false;
        }
    }
    return true;
}

}  // namespace astra::module
