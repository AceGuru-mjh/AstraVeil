package com.astraveil.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * Crash report captured when the daemon or a module faults.
 *
 * Sent to the AstraUI diagnostics panel and (optionally) to the
 * AstraUpdate telemetry endpoint.
 */
@Serializable
data class CrashReport(
    val version: String = "",
    val android: String = "",
    val device: String = "",
    val daemonState: String = "",
    val error: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
