package com.astraveil.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * MVP-level system health snapshot for the Dashboard.
 *
 * Aggregates daemon state, active provider, root availability, and
 * capability list into one object the UI renders directly.
 */
@Serializable
data class SystemHealthStatus(
    val daemonOnline: Boolean = false,
    val provider: String = "none",
    val rootAvailable: Boolean = false,
    val capabilities: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    companion object {
        fun offline(): SystemHealthStatus = SystemHealthStatus(
            daemonOnline = false,
            provider = "none",
            rootAvailable = false,
        )
    }
}
