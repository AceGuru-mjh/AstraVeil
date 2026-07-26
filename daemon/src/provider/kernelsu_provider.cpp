#include "astra/provider/kernelsu_provider.hpp"

#include "astra/capability/capability.hpp"

#include <filesystem>

namespace astra::provider {

RootType KernelSUProvider::type() const {
    return RootType::KERNELSU;
}

bool KernelSUProvider::available() const {
    return std::filesystem::exists("/data/adb/ksu");
}

bool KernelSUProvider::execute(
    const std::string&,
    std::string& output
) {
    if (!available()) {
        return false;
    }

    output = "KernelSU execution placeholder";
    return true;
}

std::string KernelSUProvider::name() const {
    return "KernelSU";
}

std::vector<capability::Capability> KernelSUProvider::capabilities() const {
    // KernelSU runs in-kernel: it has a real KERNEL_INTERFACE surface,
    // root, mount namespaces, and SELinux control. Boot patching is
    // done via a patched kernel image.
    return {
        capability::Capability::ROOT_ACCESS,
        capability::Capability::MOUNT_NAMESPACE,
        capability::Capability::KERNEL_INTERFACE,
        capability::Capability::BOOT_PATCH,
        capability::Capability::SELINUX_CONTROL,
    };
}

}  // namespace astra::provider
