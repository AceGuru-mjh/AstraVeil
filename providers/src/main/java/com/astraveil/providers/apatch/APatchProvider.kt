package com.astraveil.providers.apatch

import com.astraveil.providers.ProviderCapability
import com.astraveil.providers.ProviderExecResult
import com.astraveil.providers.RootInfo
import com.astraveil.providers.RootProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [RootProvider] backed by **APatch** (bmax121/APatch).
 *
 * APatch is a kernel-patch based root solution: it patches the running kernel
 * image in-place (no reboot required on most devices) and exposes a controlled
 * `su` through the userspace `apd` daemon. Because the patching happens at the
 * kernel image level, APatch can coexist with selinux policies that would
 * block a Magisk-style overlay.
 *
 * Detection probes:
 *  1. `/data/adb/ap/` — the daemon's persistent state directory, OR
 *  2. `/system/bin/apd` — the daemon binary shipped in the system image.
 *
 * Version metadata is read from `apd --version` when available. Commands are
 * executed via `apd su -c <cmd>`.
 */
class APatchProvider : RootProvider {

    override val id: String = "apatch"
    override val displayName: String = "APatch"

    private val apDir = File("/data/adb/ap")
    private val apdBin = File("/system/bin/apd")

    @Volatile private var cached: RootInfo = RootInfo.none()

    override suspend fun available(): Boolean = withContext(Dispatchers.IO) {
        apDir.exists() || apdBin.exists()
    }

    override suspend fun detect(): RootInfo = withContext(Dispatchers.IO) {
        if (!available()) {
            cached = RootInfo.none()
            return@withContext cached
        }
        val (ver, code) = parseApdVersion()
        cached = RootInfo(
            providerName = id,
            displayName = displayName,
            version = ver,
            versionCode = code,
            suAvailable = true,
            modulePath = "/data/adb/ap/modules",
            supportedFeatures = setOf("mount", "namespace"),
            detected = true
        )
        cached
    }

    override suspend fun info(): RootInfo = cached

    override suspend fun execute(command: String): ProviderExecResult =
        withContext(Dispatchers.IO) {
            runProcess(arrayOf("apd", "su", "-c", command))
        }

    override suspend fun mount(
        source: String,
        target: String,
        options: String
    ): Boolean = withContext(Dispatchers.IO) {
        // TODO(Phase 1): route through APatch's overlay manager so the mount
        // is reflected in the module system and persists across reboots.
        val result = runProcess(arrayOf("apd", "su", "-c", "mount -o $options $source $target"))
        result.success
    }

    // v3 capability surface.
    override suspend fun capabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.ROOT_EXECUTION,
        ProviderCapability.MOUNT_NAMESPACE,
        ProviderCapability.BOOT_PATCH,
        ProviderCapability.SELINUX_CONTROL,
    )

    // ---- internals --------------------------------------------------------

    /**
     * Run `apd --version` and parse the result. APatch's `apd` historically
     * prints `APatch <version> (<code>)` on a single line; we tolerate minor
     * formatting drift and fall back to `"unknown" / 0` when parsing fails.
     */
    private fun parseApdVersion(): Pair<String, Int> {
        if (!apdBin.exists()) return "unknown" to 0
        val out = runProcess(arrayOf("apd", "--version")).stdout.trim()
        if (out.isBlank()) return "unknown" to 0
        val ver = out.split(Regex("\\s+")).getOrNull(1) ?: "unknown"
        val code = Regex("""\((\d+)\)""").find(out)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        return ver to code
    }

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
