#include "astra/capability/capability.hpp"

namespace astra::capability {

std::string capability_name(Capability c) {
    switch (c) {
        case Capability::ROOT_ACCESS:      return "ROOT_ACCESS";
        case Capability::SYSTEM_WRITE:     return "SYSTEM_WRITE";
        case Capability::MOUNT_NAMESPACE:  return "MOUNT_NAMESPACE";
        case Capability::KERNEL_INTERFACE: return "KERNEL_INTERFACE";
        case Capability::BOOT_PATCH:       return "BOOT_PATCH";
        case Capability::SELINUX_CONTROL:  return "SELINUX_CONTROL";
        case Capability::NETWORK:          return "NETWORK";
        case Capability::IPC_ACCESS:       return "IPC_ACCESS";
        case Capability::MODULE_RUNTIME:      return "MODULE_RUNTIME";
        case Capability::NAMESPACE_ISOLATION: return "NAMESPACE_ISOLATION";
        case Capability::ASTRA_ROOT:          return "ASTRA_ROOT";
        case Capability::OVERLAYFS:           return "OVERLAYFS";
    }
    return "UNKNOWN";
}

std::vector<Capability> all_capabilities() {
    return {
        Capability::ROOT_ACCESS,
        Capability::SYSTEM_WRITE,
        Capability::MOUNT_NAMESPACE,
        Capability::KERNEL_INTERFACE,
        Capability::BOOT_PATCH,
        Capability::SELINUX_CONTROL,
        Capability::NETWORK,
        Capability::IPC_ACCESS,
        Capability::MODULE_RUNTIME,
        Capability::NAMESPACE_ISOLATION,
        Capability::ASTRA_ROOT,
        Capability::OVERLAYFS,
    };
}

}  // namespace astra::capability
