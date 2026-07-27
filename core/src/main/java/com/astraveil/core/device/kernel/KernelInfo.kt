package com.astraveil.core.device.kernel

import kotlinx.serialization.Serializable

@Serializable
data class KernelInfo(
    val version: String = "",
    val architecture: String = "",
    val overlayFs: Boolean = false,
    val ebpf: Boolean = false,
    val landlock: Boolean = false,
) {
    companion object {
        fun unknown() = KernelInfo()
    }
}
