#pragma once

#include <string>

namespace astra::security {

/// Fine-grained permission tokens understood by the AstraVeil security
/// framework.
///
/// These are the **security-layer** permission names that appear in an AVM
/// module's manifest and that the user grants or denies via the Permission
/// Center UI. Each token maps to a [astra::capability::Capability] that the
/// [SecurityEngine] checks against the device's [CapabilityMatrix] before
/// authorising.
///
/// This is deliberately finer-grained than the old single ROOT_ACCESS flag:
/// a module can be granted MOUNT without being granted BOOT_PATCH, etc.
enum class Permission {
    ROOT_ACCESS,        ///< elevate to uid 0
    SYSTEM_WRITE,       ///< write /system (via overlay/bind)
    MOUNT,              ///< create mount namespaces / bind mounts
    BOOT_PATCH,         ///< patch boot / vendor_boot / init_boot
    SELINUX_CONTROL,    ///< set SELinux mode / policies
    KERNEL_INTERFACE,   ///< direct kernel hook / syscall surface
    NETWORK_ACCESS,     ///< open sockets
    FILESYSTEM_ACCESS,  ///< arbitrary filesystem read/write
    IPC_ACCESS,         ///< provider-brokered IPC
};

/// Human-readable name of a permission, e.g. "ROOT_ACCESS".
/// Stable across releases — used as the JSON key in IPC, in module
/// manifests, and in the audit log.
std::string permission_name(Permission p);

/// Resolve a permission name string back to a [Permission].
/// Returns false if @p name does not match any known permission.
bool permission_from_name(
    const std::string& name,
    Permission& out
);

/// A single authorisation request raised by a module.
///
/// @property module_id  The AVM module requesting the permission.
/// @property permission The [Permission] being requested.
/// @property reason     Human-readable rationale shown in the AstraUI
///                      permission dialog.
struct PermissionRequest {
    std::string module_id;
    Permission permission;
    std::string reason;
};

}  // namespace astra::security
