#include "astra/root/boot_manager.hpp"

#include <filesystem>

#include "astra/logger/logger.hpp"

namespace astra::root {

BootManager::BootManager() = default;
BootManager::~BootManager() = default;

BootImage BootManager::detect() {
    /*
     * Android 10-11  → boot.img
     * Android 12     → vendor_boot
     * Android 13+    → init_boot
     *
     * Detection by presence of the partition node under /dev/block/by-name.
     */
    const auto has = [](const char* p) {
        return std::filesystem::exists(p);
    };
    if (has("/dev/block/by-name/init_boot")) {
        ALOGI("BootManager: init_boot detected (Android 13+)");
        return BootImage::INIT_BOOT;
    }
    if (has("/dev/block/by-name/vendor_boot")) {
        ALOGI("BootManager: vendor_boot detected (Android 12+)");
        return BootImage::VENDOR_BOOT;
    }
    if (has("/dev/block/by-name/boot")) {
        ALOGI("BootManager: boot.img detected (Android 10-11)");
        return BootImage::BOOT_IMG;
    }
    ALOGW("BootManager: no boot image detected");
    return BootImage::UNKNOWN;
}

bool BootManager::patch() {
    /*
     * TODO(Phase 8.x): real boot image patching:
     *   1. unpack the detected image (mkbootimg / magiskboot semantics)
     *   2. inject the AstraRoot ramdisk init hook
     *   3. repack and flash to the inactive slot (A/B)
     *   4. record a snapshot for [restore]
     *
     * Phase 8 skeleton: report the detected image and return true so
     * the provider capability surface (BOOT_PATCH) is exercised.
     */
    const auto img = detect();
    if (img == BootImage::UNKNOWN) {
        return false;
    }
    ALOGI("BootManager: patch stub for image type %d",
          static_cast<int>(img));
    return true;
}

bool BootManager::restore() {
    // TODO(Phase 8.x): flash the snapshot taken at patch() time.
    ALOGI("BootManager: restore stub");
    return true;
}

}  // namespace astra::root
