package com.astraveil.core.device

import kotlinx.serialization.Serializable

@Serializable
data class DeviceProfile(
    val manufacturer: String = "",
    val brand: String = "",
    val model: String = "",
    val androidSdk: Int = 0,
    val androidVersion: String = "",
    val kernelVersion: String = "",
    val kernelOverlayFs: Boolean = false,
    val kernelEbpf: Boolean = false,
    val kernelLandlock: Boolean = false,
    val bootUnlocked: Boolean = false,
    val bootVerifiedBoot: String = "unknown",
    val selinuxMode: String = "unknown",
    val selinuxEnforcing: Boolean = false,
    val selinuxPolicyVersion: Int = 0,
) {
    companion object {
        fun empty() = DeviceProfile()
    }
}
