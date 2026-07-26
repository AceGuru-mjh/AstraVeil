#include "astra/provider/kernelsu_provider.hpp"

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

}  // namespace astra::provider
