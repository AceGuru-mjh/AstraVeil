package com.astraveil.core.capability

import android.os.Build
import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Engine that probes the running device to assemble a [CapabilityInfo] snapshot.
 *
 * The engine is intentionally root-free: every detection path uses only
 * facilities that an unprivileged application can read (procfs, sysfs, and
 * [android.os.Build]). This lets the engine be exercised on stock devices and
 * emulators without modification, and lets the rest of the engine decide
 * whether to attempt a real provider handshake.
 *
 * Construct one instance per [com.astraveil.core.AstraCore]; the class is
 * cheap to create and holds no long-lived state.
 */
class CapabilityEngine {

    /** Pretty-printing JSON encoder used by [toJson]. */
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Common locations of the `su` binary that imply a root provider is
     * present. None of these paths require root to stat() from an unprivileged
     * application.
     */
    private val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
    )

    /**
     * Probe the device and assemble a [CapabilityInfo] snapshot.
     *
     * All blocking I/O (procfs, sysfs) is performed on [Dispatchers.IO] so
     * callers may safely invoke this from the main thread via a coroutine.
     *
     * @return A freshly populated [CapabilityInfo].
     */
    suspend fun scan(): CapabilityInfo = withContext(Dispatchers.IO) {
        val (selinuxStatus, selinuxMode) = readSelinuxStatus()
        val kernelVersion = readKernelVersion()
        val rootAvailable = detectRoot()
        val rootProvider = guessRootProvider(rootAvailable)
        CapabilityInfo(
            androidVersion = Build.VERSION.RELEASE ?: "",
            apiLevel = Build.VERSION.SDK_INT,
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "",
            abis = Build.SUPPORTED_ABIS.toList(),
            kernelVersion = kernelVersion,
            selinuxStatus = selinuxStatus,
            selinuxMode = selinuxMode,
            rootAvailable = rootAvailable,
            rootProvider = rootProvider,
            mountCapability = isMountReadable(),
            overlayFsCapability = hasOverlayFs(),
            namespaceCapability = File("/proc/self/ns/mnt").exists(),
            pidNamespace = File("/proc/self/ns/pid").exists(),
            deviceModel = Build.MODEL ?: "",
            deviceManufacturer = Build.MANUFACTURER ?: "",
            deviceBrand = Build.BRAND ?: "",
            fingerprint = Build.FINGERPRINT ?: "",
        )
    }

    /**
     * Serialize [info] to a pretty-printed JSON string.
     *
     * The output is suitable for direct persistence or transport over a
     * root-provider IPC channel.
     */
    fun toJson(info: CapabilityInfo): String =
        json.encodeToString(CapabilityInfo.serializer(), info)

    /**
     * Parse the kernel version out of `/proc/version`. The line typically
     * looks like `Linux version 5.15.149-android13-8-... (user@host) ...`.
     *
     * Returns an empty string when the file is unreadable or malformed.
     */
    private fun readKernelVersion(): String = try {
        File("/proc/version").bufferedReader().use { reader ->
            val line = reader.readLine() ?: return@use ""
            val marker = "Linux version "
            val start = line.indexOf(marker)
            if (start < 0) return@use line.trim()
            val rest = line.substring(start + marker.length)
            val firstSpace = rest.indexOf(' ')
            if (firstSpace < 0) rest.trim() else rest.substring(0, firstSpace).trim()
        }
    } catch (t: Throwable) {
        AstraLogger.w("CapabilityEngine", "Failed to read /proc/version: ${t.message}")
        ""
    }

    /**
     * Read SELinux enforcement state from `/sys/fs/selinux/enforce`.
     *
     * Missing SELinux directory means SELinux is disabled on the device.
     *
     * @return Pair of (status, raw mode string).
     */
    private fun readSelinuxStatus(): Pair<SelinuxStatus, String> {
        val selinuxDir = File("/sys/fs/selinux")
        if (!selinuxDir.exists()) return SelinuxStatus.DISABLED to ""
        val enforce = File(selinuxDir, "enforce")
        if (!enforce.exists()) return SelinuxStatus.DISABLED to ""
        return try {
            val raw = enforce.bufferedReader().use { it.readLine() ?: "" }.trim()
            val status = when (raw) {
                "1" -> SelinuxStatus.ENFORCING
                "0" -> SelinuxStatus.PERMISSIVE
                else -> SelinuxStatus.UNKNOWN
            }
            status to raw
        } catch (t: Throwable) {
            AstraLogger.w("CapabilityEngine", "Failed to read selinux enforce: ${t.message}")
            SelinuxStatus.UNKNOWN to ""
        }
    }

    /**
     * Detect presence of `su` by walking [suPaths] and (best-effort)
     * consulting the `which` utility. No privilege is required for these
     * probes; failures are silently treated as "not detected".
     */
    private fun detectRoot(): Boolean {
        suPaths.forEach { path ->
            if (File(path).exists()) {
                AstraLogger.i("CapabilityEngine", "su binary detected at $path")
                return true
            }
        }
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val out = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            if (out.isNotEmpty() && File(out).exists()) {
                AstraLogger.i("CapabilityEngine", "su located via which: $out")
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            AstraLogger.d("CapabilityEngine", "which su unavailable: ${t.message}")
            false
        }
    }

    /**
     * Map a detected `su` to a best-effort provider name. Without root this
     * is limited to filesystem hints (e.g. the Magisk `/data/adb/magisk`
     * directory). Falls back to "unknown" when nothing matches.
     */
    private fun guessRootProvider(rootAvailable: Boolean): String {
        if (!rootAvailable) return "none"
        return when {
            File("/data/adb/magisk").exists() -> "magisk"
            File("/data/adb/ksu").exists() -> "kernelsu"
            File("/data/adb/ap").exists() -> "apatch"
            File("/data/adb/astraroot").exists() -> "astraroot"
            else -> "unknown"
        }
    }

    /** Check whether `/proc/mounts` is readable. */
    private fun isMountReadable(): Boolean = try {
        File("/proc/mounts").bufferedReader().use { it.readLine(); true }
    } catch (t: Throwable) {
        AstraLogger.d("CapabilityEngine", "/proc/mounts not readable: ${t.message}")
        false
    }

    /**
     * Look for `overlay` / `overlayfs` entries in `/proc/filesystems`. Each
     * line is of the form `nodev\toverlay` or `\toverlayfs`, so we compare
     * the last token after the final tab.
     */
    private fun hasOverlayFs(): Boolean = try {
        File("/proc/filesystems").bufferedReader().use { reader ->
            reader.lineSequence().any { line ->
                val token = line.substringAfterLast('\t').trim()
                token == "overlay" || token == "overlayfs"
            }
        }
    } catch (t: Throwable) {
        AstraLogger.d("CapabilityEngine", "/proc/filesystems not readable: ${t.message}")
        false
    }
}
