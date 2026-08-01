package com.astraveil.providers.magisk

import com.astraveil.providers.ProviderCapability
import com.astraveil.providers.ProviderExecResult
import com.astraveil.providers.RootInfo
import com.astraveil.providers.RootProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [RootProvider] backed by **Magisk** (topjohnwu/Magisk).
 *
 * Detection strategy:
 *  1. `/data/adb/magisk/` exists on a rooted device, OR
 *  2. `/system/bin/magisk` is on PATH (rare; system-as-root), OR
 *  3. `which magisk` resolves inside a shell.
 *
 * When found, we attempt to parse `MAGISK_VER_CODE` / `MAGISK_VER` out of
 * `/data/adb/magisk/util_functions.sh` — that file is the canonical source of
 * Magisk's version metadata and is stable across releases. If parsing fails we
 * still report a detected backend with `version = "unknown"`.
 *
 * Command execution uses `su -c <cmd>` via [Runtime.exec]; all IO is dispatched
 * onto [Dispatchers.IO] so callers never block a UI thread.
 */
class MagiskProvider : RootProvider {

    override val id: String = "magisk"
    override val displayName: String = "Magisk"

    /** Well-known on-disk markers used by the available/detect probes. */
    private val magiskDir = File("/data/adb/magisk")
    private val utilFunctions = File("/data/adb/magisk/util_functions.sh")

    @Volatile private var cached: RootInfo = RootInfo.none()

    override suspend fun available(): Boolean = withContext(Dispatchers.IO) {
        magiskDir.exists() ||
            File("/system/bin/magisk").exists() ||
            runWhich("magisk").isNotBlank()
    }

    override suspend fun detect(): RootInfo = withContext(Dispatchers.IO) {
        if (!available()) {
            cached = RootInfo.none()
            return@withContext cached
        }
        val (ver, code) = parseUtilFunctions()
        cached = RootInfo(
            providerName = id,
            displayName = displayName,
            version = ver,
            versionCode = code,
            suAvailable = true,
            modulePath = "/data/adb/modules",
            supportedFeatures = setOf("mount", "namespace", "hide"),
            detected = true
        )
        cached
    }

    override suspend fun info(): RootInfo = cached

    @Suppress("DEPRECATION")
    override suspend fun execute(command: String): ProviderExecResult =
        withContext(Dispatchers.IO) {
            runProcess(arrayOf("su", "-c", command))
        }

    override suspend fun mount(
        source: String,
        target: String,
        options: String
    ): Boolean = withContext(Dispatchers.IO) {
        // TODO(Phase 1): route through magisk's `magisk --mount` helpers rather
        // than a raw `mount` invocation so the bind is tracked by MagiskHide.
        val result = runProcess(arrayOf("su", "-c", "mount -o $options $source $target"))
        result.success
    }

    // v3 capability surface.
    override suspend fun capabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.ROOT_EXECUTION,
        ProviderCapability.MOUNT_NAMESPACE,
        ProviderCapability.OVERLAY_FS,
        ProviderCapability.SYSTEM_PROPERTY,
        ProviderCapability.BOOT_PATCH,
        ProviderCapability.SELINUX_CONTROL,
    )

    // ---- internals --------------------------------------------------------

    /**
     * Parse `MAGISK_VER_CODE` and `MAGISK_VER` out of `util_functions.sh`.
     * Returns `"unknown" / 0` if the file is missing or doesn't contain the
     * expected lines.
     */
    private fun parseUtilFunctions(): Pair<String, Int> {
        if (!utilFunctions.exists()) return "unknown" to 0
        val text = runCatching { utilFunctions.readText() }.getOrNull()
            ?: return "unknown" to 0
        val ver = Regex("""MAGISK_VER="?([^"\n]+)"?""").find(text)
            ?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val code = Regex("""MAGISK_VER_CODE=(\d+)""").find(text)
            ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        return (if (ver.isBlank()) "unknown" else ver) to code
    }

    /** Run `which <binary>` and return the first resolved path, or `""`. */
    private fun runWhich(binary: String): String =
        runProcess(arrayOf("sh", "-c", "which $binary")).stdout.trim()

    /** Spawn a process, drain stdout/stderr, and return a [ProviderExecResult]. */
    private fun runProcess(cmd: Array<String>): ProviderExecResult {
        return try {
            val proc = ProcessBuilder(*cmd)
                .redirectErrorStream(false)
                .start()
            val stdout = proc.inputStream.bufferedReader().readText()
            val stderr = proc.errorStream.bufferedReader().readText()
            val exit = proc.waitFor()
            ProviderExecResult(exit, stdout, stderr, exit == 0)
        } catch (t: Throwable) {
            ProviderExecResult(-1, "", t.message ?: t.javaClass.simpleName, false)
        }
    }
}
