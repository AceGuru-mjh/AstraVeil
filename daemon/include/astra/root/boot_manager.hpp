#pragma once

namespace astra::root {

/// Which boot image this device boots from.
///
/// Android boot layout evolved across versions:
///   Android 10-11  → boot.img only
///   Android 12     → boot.img + vendor_boot
///   Android 13+    → boot.img shrinks; ramdisk moves to init_boot
enum class BootImage {
    UNKNOWN,
    BOOT_IMG,
    VENDOR_BOOT,
    INIT_BOOT,
};

/// Abstracts boot-image patching so AstraRoot does not bind to a single
/// boot layout. [detect] inspects the device to decide which image to
/// patch; [patch] writes the AstraRoot ramdisk hook; [restore] reverts.
class BootManager {
public:
    BootManager();
    ~BootManager();

    /// Inspect the device and report which boot image is in use.
    BootImage detect();

    /// Patch the detected boot image to install AstraRoot's init hook.
    /// Returns false if no supported image is found.
    bool patch();

    /// Restore the original boot image (rollback).
    bool restore();
};

}  // namespace astra::root
