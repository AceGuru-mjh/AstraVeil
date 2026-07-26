package com.astraveil.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * Startup diagnostic report — every subsystem the daemon / app checks
 * before declaring itself ready. Shown on the AstraUI Alpha diagnostics
 * screen.
 */
@Serializable
data class DiagnosticReport(
    val daemon: Boolean = false,
    val provider: Boolean = false,
    val security: Boolean = false,
    val sandbox: Boolean = false,
    val moduleRuntime: Boolean = false,
    val warnings: List<String> = emptyList(),
) {
    /** True iff every critical subsystem is up. */
    val allHealthy: Boolean get() = daemon && provider && security && sandbox && moduleRuntime
}
