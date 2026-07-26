package com.astraveil.core.device

import kotlinx.serialization.Serializable

/**
 * Frozen Android device profile — the device side of the v3 capability
 * equation (device + kernel + SELinux + boot + provider).
 *
 * Produced by [DeviceInspector.inspect] from `Build.*` + `getenforce`.
 */
@Serializable
data class AndroidProfile(
    val sdk: Int,
    val androidVersion: String,
    val manufacturer: String,
    val model: String,
    val kernel: String,
    val selinux: String,
) {
    companion object {
        fun empty(): AndroidProfile = AndroidProfile(
            sdk = 0,
            androidVersion = "",
            manufacturer = "",
            model = "",
            kernel = "",
            selinux = "",
        )
    }
}
