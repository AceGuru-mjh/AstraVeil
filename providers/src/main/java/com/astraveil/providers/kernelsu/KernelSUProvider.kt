package com.astraveil.providers.kernelsu

import com.astraveil.providers.ProviderCapability
import com.astraveil.providers.ProviderExecResult
import com.astraveil.providers.RootInfo
import com.astraveil.providers.RootProvider
import com.astraveil.core.modules.model.ModuleInfo
import com.astraveil.core.modules.model.ModuleUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [RootProvider] backed by **KernelSU** (tiann/KernelSU).
 *
 * Unlike Magisk, KernelSU performs root enforcement inside the kernel itself:
 * a patch to the kernel's `prctl`/syscall path exposes a controlled `su`
 * command via the userspace `ksud` daemon. The kernel-side origin means
 * KernelSU is significantly harder to detect from userspace heuristics alone,
 * so AstraVeil probes two artifacts:
 *
 *  1. `/data/adb/ksu/` — the daemon's persistent state directory, OR
 *  2. `/system/bin/ksud` — the daemon binary shipped in the system image.
 *
 * Version metadata is read from `ksud --version` when available. Commands are
 * executed via `ksud su -c <cmd>`; AstraVeil never invokes the kernel syscall
 * directly — that path is reserved for the future AstraRoot backend.
 */
class KernelSUProvider : RootProvider {

    override val id: String = "kernelsu"
    override val displayName: String = "KernelSU"

    private val ksuDir = File("/data/adb/ksu")
    private val ksudBin = File("/system/bin/ksud")

    @Volatile private var cached: RootInfo = RootInfo.none()

    override suspend fun available(): Boolean = withContext(Dispatchers.IO) {
        ksuDir.exists() || ksudBin.exists()
    }

    override suspend fun detect(): RootInfo = withContext(Dispatchers.IO) {
        if (!available()) {
            cached = RootInfo.none()
            return@withContext cached
        }
        val (ver, code) = parseKsudVersion()
        cached = RootInfo(
            providerName = id,
            displayName = displayName,
            version = ver,
            versionCode = code,
            suAvailable = true,
            modulePath = "/data/adb/ksu/modules",
            supportedFeatures = setOf("mount", "namespace"),
            detected = true
        )
        cached
    }

    override suspend fun info(): RootInfo = cached

    @Suppress("DEPRECATION")
    override suspend fun execute(command: String): ProviderExecResult =
        withContext(Dispatchers.IO) {
            runProcess(arrayOf("ksud", "su", "-c", command))
        }

    override suspend fun mount(
        source: String,
        target: String,
        options: String
    ): Boolean = withContext(Dispatchers.IO) {
        // TODO(Phase 1): delegate to KernelSU's overlayfs-based mount manager
        // so the mount survives reboot and is tracked by the module system.
        val result = runProcess(arrayOf("ksud", "su", "-c", "mount -o $options $source $target"))
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
     * Run `ksud --version` and parse the result. Format is backend-defined but
     * historically `KernelSU <version> (<code>)`, e.g.
     * `KernelSU 0.7.7 (120)`. We tolerate any whitespace-separated tail.
     */
    private fun parseKsudVersion(): Pair<String, Int> {
        if (!ksudBin.exists()) return "unknown" to 0
        val out = runProcess(arrayOf("ksud", "--version")).stdout.trim()
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

    // ====================================================================
    //  Module management
    //  KernelSU shares /data/adb/modules/ with Magisk.
    //  Install command: ksud module install <path>
    // ====================================================================

    override suspend fun listModules(): List<ModuleInfo> {
        if (!available()) return emptyList()
        return withContext(Dispatchers.IO) {
            val script = buildString {
                appendLine("for d in /data/adb/modules/*/; do")
                appendLine("  [ -f \"\${d}module.prop\" ] || continue")
                appendLine("  id=\$(grep '^id=' \"\${d}module.prop\" | head -1 | cut -d= -f2-)")
                appendLine("  name=\$(grep '^name=' \"\${d}module.prop\" | head -1 | cut -d= -f2-)")
                appendLine("  version=\$(grep '^version=' \"\${d}module.prop\" | head -1 | cut -d= -f2-)")
                appendLine("  author=\$(grep '^author=' \"\${d}module.prop\" | head -1 | cut -d= -f2-)")
                appendLine("  desc=\$(grep '^description=' \"\${d}module.prop\" | head -1 | cut -d= -f2-)")
                appendLine("  enabled=true")
                appendLine("  [ -f \"\${d}disable\" ] && enabled=false")
                appendLine("  echo \"\${id}|\${name}|\${version}|\${author}|\${desc}|\${enabled}\"")
                appendLine("done")
            }
            @Suppress("DEPRECATION")
            val result = execute(script)
            if (!result.success) return@withContext emptyList()
            result.stdout.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.contains("|") }
                .mapNotNull { line ->
                    val parts = line.split("|", limit = 6)
                    if (parts.size < 6) return@mapNotNull null
                    ModuleInfo(
                        id = parts[0].trim(),
                        name = parts[1].trim(),
                        version = parts[2].trim(),
                        description = parts[4].trim(),
                        state = if (parts[5].trim() == "true") ModuleUiState.RUNNING else ModuleUiState.STOPPED,
                        permissions = emptyList(),
                    )
                }
                .toList()
        }
    }

    override suspend fun installModule(zipFile: File): ModuleInfo {
        if (!available()) throw IllegalStateException("KernelSU not available")
        return withContext(Dispatchers.IO) {
            val remotePath = "/data/local/tmp/${zipFile.name}"
            @Suppress("DEPRECATION")
            execute("cp ${zipFile.absolutePath} $remotePath 2>/dev/null || true")
            @Suppress("DEPRECATION")
            val result = execute("ksud module install $remotePath")
            @Suppress("DEPRECATION")
            execute("rm -f $remotePath")
            if (!result.success) {
                throw IllegalStateException(
                    "KernelSU module install failed: ${result.stderr.ifBlank { result.stdout }}",
                )
            }
            val id = zipFile.nameWithoutExtension
            ModuleInfo(
                id = id, name = id, version = "",
                description = "Installed via AstraVeil",
                state = ModuleUiState.INSTALLED,
                permissions = emptyList(),
            )
        }
    }

    override suspend fun removeModule(id: String): Boolean {
        if (!available()) return false
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            execute("rm -rf /data/adb/modules/$id").success
        }
    }

    override suspend fun setModuleState(id: String, enabled: Boolean): Boolean {
        if (!available()) return false
        return withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            val result = if (enabled) {
                execute("rm -f /data/adb/modules/$id/disable")
            } else {
                execute("touch /data/adb/modules/$id/disable")
            }
            result.success
        }
    }
}
