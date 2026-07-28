package com.astraveil.core.diagnostics

import android.content.Context
import com.astraveil.core.device.DeviceInspector
import com.astraveil.core.compatibility.CompatibilityEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Diagnostic report generator & exporter for AstraVeil.
 * Collects details from DeviceInspector, CompatibilityEngine, and the active provider,
 * formatting them into a high-fidelity system assessment report (.astra-report).
 */
class ReportExporter(private val context: Context) {

    suspend fun generateReport(activeProviderName: String, providerVersion: String): String = withContext(Dispatchers.IO) {
        val inspector = DeviceInspector()
        val profile = inspector.inspect()
        val compatibilityEngine = CompatibilityEngine()
        val compatResult = compatibilityEngine.evaluate(profile)

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("       ASTRAVEIL DIAGNOSTIC REPORT       \n")
        sb.append("=========================================\n\n")
        sb.append("[Device Information]\n")
        sb.append("Manufacturer: ${profile.manufacturer}\n")
        sb.append("Brand: ${profile.brand}\n")
        sb.append("Model: ${profile.model}\n")
        sb.append("Android SDK: ${profile.androidSdk}\n")
        sb.append("Android Version: ${profile.androidVersion}\n\n")

        sb.append("[Kernel Intelligence]\n")
        sb.append("Kernel Version: ${profile.kernelVersion}\n")
        sb.append("OverlayFS Support: ${profile.kernelOverlayFs}\n")
        sb.append("eBPF Support: ${profile.kernelEbpf}\n")
        sb.append("Landlock Support: ${profile.kernelLandlock}\n\n")

        sb.append("[Boot & Security State]\n")
        sb.append("Bootloader Unlocked: ${profile.bootUnlocked}\n")
        sb.append("Verified Boot State: ${profile.bootVerifiedBoot}\n")
        sb.append("SELinux Mode: ${profile.selinuxMode}\n")
        sb.append("SELinux Enforcing: ${profile.selinuxEnforcing}\n")
        sb.append("SELinux Policy Version: ${profile.selinuxPolicyVersion}\n\n")

        sb.append("[Active Privilege Backend]\n")
        sb.append("Active Provider: $activeProviderName\n")
        sb.append("Provider Version: $providerVersion\n\n")

        sb.append("[Compatibility Evaluation]\n")
        sb.append("Overall Level: ${compatResult.level}\n")
        sb.append("Integrity Score: ${compatResult.score}/100\n\n")

        sb.append("[Warnings & Restrictions]\n")
        if (compatResult.warnings.isEmpty()) {
            sb.append("No active system restrictions detected.\n")
        } else {
            compatResult.warnings.forEach { sb.append("- $it\n") }
        }

        sb.append("\nReport Generated Successfully.")
        sb.toString()
    }

    suspend fun exportToFile(activeProviderName: String, providerVersion: String): File = withContext(Dispatchers.IO) {
        val reportContent = generateReport(activeProviderName, providerVersion)
        val file = File(context.filesDir, "diagnostics.astra-report")
        file.writeText(reportContent)
        file
    }
}
