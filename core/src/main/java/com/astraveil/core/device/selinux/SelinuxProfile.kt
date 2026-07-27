package com.astraveil.core.device.selinux

import kotlinx.serialization.Serializable

@Serializable
data class SelinuxProfile(
    val mode: String = "unknown",
    val enforcing: Boolean = false,
    val policyVersion: Int = 0,
) {
    companion object {
        fun unknown() = SelinuxProfile()
    }
}
