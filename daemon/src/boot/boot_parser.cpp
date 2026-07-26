#include "astra/boot/boot_parser.hpp"

#include "astra/logger/logger.hpp"

#include <cstring>
#include <fstream>
#include <vector>

namespace astra::boot {

namespace {

// Android boot image magic (boot_img v0+).
constexpr char kBootMagic[] = "ANDROID!";
constexpr std::size_t kBootMagicLen = 8;

}  // namespace

BootInfo BootParser::parse(const std::string& path) {
    BootInfo info;
    std::ifstream f(path, std::ios::binary);
    if (!f.is_open()) {
        ALOGW("BootParser: cannot open %s", path.c_str());
        return info;
    }

    // Read the magic.
    std::vector<char> magic(kBootMagicLen);
    f.read(magic.data(), kBootMagicLen);
    if (f.gcount() != static_cast<std::streamsize>(kBootMagicLen)) {
        return info;
    }

    if (std::memcmp(magic.data(), kBootMagic, kBootMagicLen) != 0) {
        // vendor_boot uses a different magic ("VNDRBOOT"); detect it.
        if (std::memcmp(magic.data(), "VNDRBOOT", 8) == 0) {
            info.valid = true;
            info.has_ramdisk = true;
            ALOGI("BootParser: %s is a vendor_boot image", path.c_str());
            return info;
        }
        ALOGW("BootParser: %s has unknown magic", path.c_str());
        return info;
    }

    info.valid = true;
    info.has_ramdisk = true;  // boot.img v0..v3 carry a ramdisk
    ALOGI("BootParser: %s is a boot.img (Android magic)", path.c_str());
    return info;
}

}  // namespace astra::boot
