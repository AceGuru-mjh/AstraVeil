#include "astra/provider/apatch_provider.hpp"

#include "astra/capability/capability.hpp"

#include <filesystem>

namespace astra::provider {

RootType APatchProvider::type() const {
    return RootType::APATCH;
}

bool APatchProvider::available() const {
    return std::filesystem::exists("/data/adb/ap");
}

bool APatchProvider::execute(
    const std::string&,
    std::string& output
) {
    if (!available()) {
        return false;
    }

    output = "APatch execution placeholder";
    return true;
}

std::string APatchProvider::name() const {
    return "APatch";
}

std::vector<capability::Capability> APatchProvider::capabilities() const {
    // APatch patches the kernel image in place: it has a kernel
    // interface surface, root, mount namespaces, and boot patching
    // (kernel image patch is its core mechanism). SELinux control
    // is supported via its su path.
    return {
        capability::Capability::ROOT_ACCESS,
        capability::Capability::MOUNT_NAMESPACE,
        capability::Capability::KERNEL_INTERFACE,
        capability::Capability::BOOT_PATCH,
        capability::Capability::SELINUX_CONTROL,
    };
}

}  // namespace astra::provider
