#pragma once

#include <memory>
#include <string>

namespace astra::platform {

/// Android release version, matching `ro.build.version.release`.
enum class AndroidVersion {
    UNKNOWN,
    ANDROID_10,   // API 29
    ANDROID_11,   // API 30
    ANDROID_12,   // API 31/32
    ANDROID_13,   // API 33
    ANDROID_14,   // API 34
    ANDROID_15,   // API 35
    ANDROID_16,   // API 36
};

/// Per-version Android platform abstraction.
///
/// Different Android versions mount /system differently, ship different
/// boot layouts, and expose different SELinux policy surfaces.
/// [AndroidPlatform] lets the rest of AstraRoot stay version-agnostic:
/// the daemon asks "setupMount()" and the right thing happens for the
/// detected version.
class AndroidPlatform {
public:
    virtual ~AndroidPlatform() = default;

    /// The Android version this platform targets.
    virtual AndroidVersion version() const = 0;

    /// Bring up the platform-specific mount strategy.
    virtual bool setupMount() = 0;

    /// Bring up the platform-specific property surface.
    virtual bool setupProperty() = 0;

    /// Human-readable label, e.g. "Android 14".
    virtual std::string label() const = 0;
};

/// Detect the running Android version by reading /system/build.prop.
/// Returns [AndroidVersion::UNKNOWN] if the version cannot be parsed.
AndroidVersion detect_android_version();

/// Factory: returns the right [AndroidPlatform] for the detected
/// version, or nullptr on UNKNOWN.
std::unique_ptr<AndroidPlatform> make_android_platform();

}  // namespace astra::platform
