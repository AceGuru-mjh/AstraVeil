package com.astraveil.nativelib

/**
 * JNI bridge to the C++ native library (libastra_native.so).
 * Provides low-level device probing that is faster and more reliable
 * than Java File I/O for procfs/sysfs reads.
 */
object NativeBridge {

    private var loaded = false

    init {
        try {
            System.loadLibrary("astra_native")
            loaded = true
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.w("NativeBridge", "libastra_native.so not available: ${e.message}")
        }
    }

    val isAvailable: Boolean get() = loaded

    /** Kernel version string (e.g. "6.1.0-android14-8-g1234"). */
    external fun nativeGetKernelVersion(): String

    /** SELinux status: 1=enforcing, 0=permissive, -1=disabled. */
    external fun nativeGetSelinuxStatus(): Int

    /** Whether overlayfs is listed in /proc/filesystems. */
    external fun nativeHasOverlayFs(): Boolean

    /** Paths where su-like binaries were found. */
    external fun nativeScanSuPaths(): Array<String>

    /** Whether a specific namespace type exists (e.g. "mnt", "pid", "net"). */
    external fun nativeHasNamespace(nsType: String): Boolean

    /** Whether /proc/config.gz exists (kernel build config available). */
    external fun nativeHasKernelConfig(): Boolean

    /**
     * Invoke a C-exported symbol in a module's .so.
     *
     * Used by [com.astraveil.modules.ModuleRuntime] to call the module
     * entry point (e.g. `avm_on_load`) after `System.load` has mapped the
     * library. The [soPath] must be the absolute path of an already-loaded
     * .so (or a path dlopen can open). The [symbol] must be an
     * `extern "C"` function taking no arguments and returning void.
     *
     * @return `true` if the symbol was found and invoked without throwing;
     *         `false` if the symbol is absent or dlopen/dlsym failed.
     */
    external fun nativeInvokeModuleEntry(soPath: String, symbol: String): Boolean
}
