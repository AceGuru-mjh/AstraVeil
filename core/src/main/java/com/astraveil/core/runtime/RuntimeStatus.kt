package com.astraveil.core.runtime

import kotlinx.serialization.Serializable

@Serializable
data class RuntimeStatus(
    val daemonOnline: Boolean = false,
    val latencyMs: Int = 0,
    val daemonVersion: String = "0.1.0",
    val pid: Int = 0,
) {
    companion object {
        fun offline() = RuntimeStatus()
    }
}
