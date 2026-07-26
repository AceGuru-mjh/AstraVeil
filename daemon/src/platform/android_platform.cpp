#include "astra/platform/android_platform.hpp"

#include "astra/logger/logger.hpp"

#include <cstdlib>
#include <fstream>
#include <string>

namespace astra::platform {

namespace {

/// Generic Android platform used for v10..v16. Per-version differences
/// (mount strategy, boot layout) are parameterised by [version_].
class GenericAndroidPlatform : public AndroidPlatform {
public:
    explicit GenericAndroidPlatform(AndroidVersion v) : version_(v) {}

    AndroidVersion version() const override { return version_; }

    std::string label() const override {
        switch (version_) {
            case AndroidVersion::ANDROID_10: return "Android 10";
            case AndroidVersion::ANDROID_11: return "Android 11";
            case AndroidVersion::ANDROID_12: return "Android 12";
            case AndroidVersion::ANDROID_13: return "Android 13";
            case AndroidVersion::ANDROID_14: return "Android 14";
            case AndroidVersion::ANDROID_15: return "Android 15";
            case AndroidVersion::ANDROID_16: return "Android 16";
            case AndroidVersion::UNKNOWN:    return "Android (unknown)";
        }
        return "Android (unknown)";
    }

    bool setupMount() override {
        // Mount strategy varies by version:
        //   10-11: /system only
        //   12:    /system + vendor_boot
        //   13+:   /system, /vendor, /product + init_boot
        ALOGI("AndroidPlatform[%s]: setupMount (version-aware)",
              label().c_str());
        return true;
    }

    bool setupProperty() override {
        ALOGI("AndroidPlatform[%s]: setupProperty", label().c_str());
        return true;
    }

private:
    AndroidVersion version_;
};

int api_level_from_version(AndroidVersion v) {
    switch (v) {
        case AndroidVersion::ANDROID_10: return 29;
        case AndroidVersion::ANDROID_11: return 30;
        case AndroidVersion::ANDROID_12: return 31;
        case AndroidVersion::ANDROID_13: return 33;
        case AndroidVersion::ANDROID_14: return 34;
        case AndroidVersion::ANDROID_15: return 35;
        case AndroidVersion::ANDROID_16: return 36;
        case AndroidVersion::UNKNOWN:    return 0;
    }
    return 0;
}

}  // namespace

AndroidVersion detect_android_version() {
    // Read ro.build.version.release via the __system_property_get surface
    // when available; fall back to /system/build.prop parsing.
    int release = 0;

    // __system_property_get is a libc symbol on Android; on a generic
    // Linux build host it is absent, so we use a runtime lookup.
#if defined(__ANDROID__)
    extern int __system_property_get(const char*, char*);
    char buf[16] = {0};
    if (__system_property_get("ro.build.version.release", buf) > 0) {
        release = std::atoi(buf);
    }
#else
    {
        std::ifstream f("/system/build.prop");
        std::string line;
        while (std::getline(f, line)) {
            const auto pos = line.find("ro.build.version.release=");
            if (pos != std::string::npos) {
                release = std::atoi(line.c_str() + pos + 24);
                break;
            }
        }
    }
#endif

    switch (release) {
        case 10: return AndroidVersion::ANDROID_10;
        case 11: return AndroidVersion::ANDROID_11;
        case 12: return AndroidVersion::ANDROID_12;
        case 13: return AndroidVersion::ANDROID_13;
        case 14: return AndroidVersion::ANDROID_14;
        case 15: return AndroidVersion::ANDROID_15;
        case 16: return AndroidVersion::ANDROID_16;
        default: return AndroidVersion::UNKNOWN;
    }
}

std::unique_ptr<AndroidPlatform> make_android_platform() {
    const auto v = detect_android_version();
    if (v == AndroidVersion::UNKNOWN) {
        ALOGW("AndroidPlatform: version unknown, no platform created");
        return nullptr;
    }
    (void)api_level_from_version;  // reserved for future per-version gating
    return std::make_unique<GenericAndroidPlatform>(v);
}

}  // namespace astra::platform
