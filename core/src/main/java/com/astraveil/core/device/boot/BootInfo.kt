package com.astraveil.core.device.boot

import kotlinx.serialization.Serializable

@Serializable
data class BootInfo(
    val unlocked: Boolean = false,
    val verifiedBoot: String = "unknown",
    val dmVerity: Boolean = false,
) {
    companion object {
        fun unknown() = BootInfo()
    }
}
