package com.astraveil.nativebridge

/**
 * NativeBridge — JNI entry points backed by `libastra_native.so`.
 *
 * The corresponding C symbols live in `native/src/main/cpp/jni_bridge.cpp`
 * with `Java_com_astraveil_nativebridge_NativeBridge_*` linkage. All methods
 * are cheap (procfs / sysfs reads) and safe to call from a background thread.
 */
object NativeBridge {

    init {
        // Loads libastra_native.so from the APK's lib/<abi>/ directory.
        System.loadLibrary("astra_native")
    }

    /** Linux kernel version string parsed from `/proc/version` (e.g. "6.1.55"). */
    external fun nativeKernelVersion(): String

    /** SELinux mode: "enforcing", "permissive", or "disabled". */
    external fun nativeSelinuxStatus(): String

    /** True if the kernel advertises `overlay` / `overlayfs` in `/proc/filesystems`. */
    external fun nativeHasOverlayFs(): Boolean

    /** True if mount-namespace support is advertised (`namespace` / `nsfs`). */
    external fun nativeHasMountNamespace(): Boolean

    /** Filesystems the running kernel knows about (from `/proc/filesystems`). */
    external fun nativeSupportedFilesystems(): Array<String>

    /** True if any well-known `su` binary path exists on the device. */
    external fun nativeSuPathExists(): Boolean
}
