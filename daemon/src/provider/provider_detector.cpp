#include "astra/provider/provider_detector.hpp"

#include "astra/provider/no_root_provider.hpp"

#include <filesystem>

namespace astra::provider {

std::unique_ptr<RootProvider>
ProviderDetector::detect() {
    /*
     * Magisk
     */
    if (std::filesystem::exists("/sbin/.magisk")) {
        // 下一阶段接入
    }

    /*
     * KernelSU
     */
    if (std::filesystem::exists("/data/adb/ksu")) {
        // 下一阶段接入
    }

    /*
     * APatch
     */
    if (std::filesystem::exists("/data/adb/ap")) {
        // 下一阶段接入
    }

    return std::make_unique<NoRootProvider>();
}

}  // namespace astra::provider
