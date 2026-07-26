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
    };
}

}  // namespace astra::capability
