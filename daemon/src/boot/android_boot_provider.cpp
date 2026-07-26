#include "astra/boot/android_boot_provider.hpp"
#include "astra/boot/boot_parser.hpp"

#include "astra/logger/logger.hpp"

#include <filesystem>

namespace astra::boot {

namespace {

const char* kBootPaths[] = {
    "/dev/block/by-name/init_boot",
    "/dev/block/by-name/vendor_boot",
    "/dev/block/by-name/boot",
    nullptr,
};

}  // namespace

bool AndroidBootProvider::detect() {
    for (int i = 0; kBootPaths[i] != nullptr; ++i) {
        if (std::filesystem::exists(kBootPaths[i])) {
            boot_path_ = kBootPaths[i];
            ALOGI("AndroidBootProvider: detected %s", boot_path_.c_str());
            return true;
        }
    }
    ALOGW("AndroidBootProvider: no boot image detected");
    return false;
}

bool AndroidBootProvider::unpack() {
    if (boot_path_.empty() && !detect()) {
        return false;
    }
    BootParser parser;
    const auto info = parser.parse(boot_path_);
    ALOGI("AndroidBootProvider: unpack valid=%d ramdisk=%d",
          info.valid ? 1 : 0, info.has_ramdisk ? 1 : 0);
    return info.valid;
}

bool AndroidBootProvider::patch() {
    if (!unpack()) {
        return false;
    }
    // TODO(Phase 9): inject AstraRoot ramdisk hook + repack.
    ALOGI("AndroidBootProvider: patch stub for %s", boot_path_.c_str());
    return true;
}

bool AndroidBootProvider::restore() {
    ALOGI("AndroidBootProvider: restore stub");
    return true;
}

}  // namespace astra::boot
