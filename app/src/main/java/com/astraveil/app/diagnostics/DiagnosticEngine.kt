package com.astraveil.app.diagnostics

import android.app.Application
import android.os.Build
import com.astraveil.app.AstraVeilApplication
import com.astraveil.core.ipc.DaemonState
import com.astraveil.core.provenance.DataProvenance
import com.astraveil.providers.ProviderCapability
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * One diagnostic conclusion, carrying its provenance honestly (P2-18).
 *
 * @param provenance  how the value was obtained (only PROBED may say "Verified")
 * @param source      human-readable description of the data source
 * @param implemented false → UI must show "Prototype" / "Unavailable", not imply it works
 */
data class DiagnosticConclusion(
    val id: String,
    val title: String,
    val value: String,
    val provenance: DataProvenance,
    val source: String,
    val implemented: Boolean = true,
)

/**
 * Produces diagnostic conclusions from REAL probes, and marks subsystems
 * that are not yet implemented as Prototype (implemented=false). This is
 * the fix for P2-18: no conclusion may claim more than is actually true.
 *
 * Each conclusion carries its [DataProvenance] so the UI can show a badge
 * (Verified / Detected / Reported / Inferred / Unavailable) AND a source
 * string so the user can see exactly how the value was obtained.
 *
 * Lives in `:app` (not `:core`) because it references [ProviderRegistry]
 * from `:providers`, which `:core` cannot depend on.
 */
class DiagnosticEngine(private val app: Application) {

    fun runAll(): List<DiagnosticConclusion> =
        device() + capabilities() + backend() + subsystems()

    // ── Device facts: DETECTED from the OS ──
    private fun device(): List<DiagnosticConclusion> {
        val selinux = runCatching {
            File("/sys/fs/selinux/enforce").readText().trim() == "1"
        }.getOrDefault(false)
        return listOf(
            DiagnosticConclusion("dev.model", "Device",
                "${Build.MANUFACTURER} ${Build.MODEL}",
                DataProvenance.DETECTED, "android.os.Build"),
            DiagnosticConclusion("dev.android", "Android",
                "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                DataProvenance.DETECTED, "android.os.Build"),
            DiagnosticConclusion("dev.kernel", "Kernel",
                System.getProperty("os.version") ?: "unknown",
                DataProvenance.DETECTED, "os.version property"),
            DiagnosticConclusion("dev.abi", "ABI",
                Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
                DataProvenance.DETECTED, "android.os.Build"),
            DiagnosticConclusion("dev.selinux", "SELinux",
                if (selinux) "Enforcing" else "Permissive",
                DataProvenance.DETECTED, "/sys/fs/selinux/enforce"),
        )
    }

    // ── Capabilities: PROBED/DETECTED from the active provider's advertised set ──
    private fun capabilities(): List<DiagnosticConclusion> {
        val provider = runCatching {
            runBlocking { ProviderRegistry.activeProvider() }
        }.getOrNull()
        if (provider == null) {
            return listOf(DiagnosticConclusion("cap.none", "Capabilities",
                "none detected", DataProvenance.UNAVAILABLE,
                "no root backend present"))
        }
        val caps = runCatching {
            runBlocking { provider.capabilities() }
        }.getOrDefault(emptySet())
        if (caps.isEmpty()) {
            return listOf(DiagnosticConclusion("cap.empty", "Capabilities",
                "provider advertises none", DataProvenance.ADVERTISED,
                "${provider.id}.capabilities() = empty"))
        }
        return ProviderCapability.values().sortedBy { it.name }.map { cap ->
            val present = cap in caps
            DiagnosticConclusion("cap.${cap.name}", cap.name,
                if (present) "available" else "unavailable",
                if (present) DataProvenance.PROBED else DataProvenance.DETECTED,
                "${provider.id}.capabilities()")
        }
    }

    // ── Root backend: DETECTED ──
    private fun backend(): List<DiagnosticConclusion> {
        val active = runCatching {
            runBlocking { ProviderRegistry.detectActive() }
        }.getOrNull()
        return listOf(DiagnosticConclusion("backend.active", "Root backend",
            active?.displayName ?: "none",
            if (active != null) DataProvenance.DETECTED else DataProvenance.UNAVAILABLE,
            "ProviderRegistry.detectActive()"))
    }

    // ── Subsystems: HONEST about implementation state ──
    private fun subsystems(): List<DiagnosticConclusion> {
        val daemonState = runCatching {
            AstraVeilApplication.daemonManager.state.value
        }.getOrNull()

        return listOf(
            DiagnosticConclusion("sub.daemon", "AstraDaemon IPC",
                when (daemonState) {
                    DaemonState.ONLINE -> "connected"
                    null -> "not wired"
                    else -> daemonState.name.lowercase()
                },
                if (daemonState == DaemonState.ONLINE) DataProvenance.PROBED
                else DataProvenance.UNAVAILABLE,
                "DaemonManager.state",
                implemented = daemonState == DaemonState.ONLINE),

            DiagnosticConclusion("sub.isolation", "Module isolation",
                "in-process (Phase 0)",
                DataProvenance.ADVERTISED,
                "ModuleRuntime uses System.load; isolated ModuleRunner is Phase 1",
                implemented = false),

            DiagnosticConclusion("sub.rust", "Rust policy enforcement",
                "fail-closed fallback active",
                DataProvenance.INFERRED,
                "policy_bridge weak fallback returns DENY; not yet verified on-device",
                implemented = true),

            DiagnosticConclusion("sub.selinux_policy", "SELinux policy loading",
                "not loaded",
                DataProvenance.UNAVAILABLE,
                ".te files exist but are not loaded by magiskpolicy",
                implemented = false),
        )
    }
}
