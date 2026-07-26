#include "astra/provider/magisk_provider.hpp"

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

}  // namespace astra::provider
