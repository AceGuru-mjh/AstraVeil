#include "astra/security/permission.hpp"

namespace astra::security {

std::string permission_name(Permission p) {
    switch (p) {
        case Permission::ROOT_ACCESS:       return "ROOT_ACCESS";
        case Permission::SYSTEM_WRITE:      return "SYSTEM_WRITE";
        case Permission::MOUNT:             return "MOUNT";
        case Permission::BOOT_PATCH:        return "BOOT_PATCH";
        case Permission::SELINUX_CONTROL:   return "SELINUX_CONTROL";
        case Permission::KERNEL_INTERFACE:  return "KERNEL_INTERFACE";
        case Permission::NETWORK_ACCESS:    return "NETWORK_ACCESS";
        case Permission::FILESYSTEM_ACCESS: return "FILESYSTEM_ACCESS";
        case Permission::IPC_ACCESS:        return "IPC_ACCESS";
    }
    return "UNKNOWN";
}

bool permission_from_name(
    const std::string& name,
    Permission& out
) {
    // Order mirrors the enum declaration.
    const Permission all[] = {
        Permission::ROOT_ACCESS,
        Permission::SYSTEM_WRITE,
        Permission::MOUNT,
        Permission::BOOT_PATCH,
        Permission::SELINUX_CONTROL,
        Permission::KERNEL_INTERFACE,
        Permission::NETWORK_ACCESS,
        Permission::FILESYSTEM_ACCESS,
        Permission::IPC_ACCESS,
    };
    for (auto p : all) {
        if (permission_name(p) == name) {
            out = p;
            return true;
        }
    }
    return false;
}

}  // namespace astra::security
