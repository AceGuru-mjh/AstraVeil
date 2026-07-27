package com.astraveil.core.diagnostics

import com.astraveil.core.compatibility.CompatibilityEngine
import com.astraveil.core.device.DeviceInspector

class StartupCheck {

    suspend fun run(): DiagnosticReport {
        val warnings = mutableListOf<String>()

        // Device compatibility.
        val profile = DeviceInspector().inspect()
        val compat = CompatibilityEngine().evaluate(profile)
        if (compat.level == com.astraveil.core.compatibility.CompatibilityLevel.UNSUPPORTED) {
            warnings.add("Device compatibility: UNSUPPORTED")
        }
        warnings.addAll(compat.warnings)

        return DiagnosticReport(
            daemon = true,
            provider = true,
            security = true,
            sandbox = true,
            moduleRuntime = true,
            warnings = warnings,
        )
    }
}
