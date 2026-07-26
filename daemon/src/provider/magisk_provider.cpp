#include "astra/provider/magisk_provider.hpp"

#include "astra/capability/capability.hpp"

#include <filesystem>

namespace astra::provider {

RootType MagiskProvider::type() const {
    return RootType::MAGISK;
}

bool MagiskProvider::available() const {
    return std::filesystem::exists("/sbin/.magisk");
}

bool MagiskProvider::execute(
    const std::string& command,
    std::string& output
) {
    if (!available()) {
        return false;
    }

    /*
     * 后续替换:
     *
     * magisk su 调用
     *
     */

    output = "Magisk execution placeholder";
    return true;
}

std::string MagiskProvider::name() const {
    return "Magisk";
}

std::vector<capability::Capability> MagiskProvider::capabilities() const {
    // Magisk offers root, overlay-based /system writes, mount namespaces,
    // boot-image patching (its core install mechanism), and SELinux
    // control. It does not expose a kernel hook surface.
    return {
        capability::Capability::ROOT_ACCESS,
        capability::Capability::SYSTEM_WRITE,
        capability::Capability::MOUNT_NAMESPACE,
        capability::Capability::BOOT_PATCH,
        capability::Capability::SELINUX_CONTROL,
    };
}

}  // namespace astra::provider
