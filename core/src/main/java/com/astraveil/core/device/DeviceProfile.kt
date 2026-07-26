package com.astraveil.core.device

import kotlinx.serialization.Serializable

/**
 * Frozen device profile.
 *
 * Android fragmentation means "Android version = capability" is wrong.
 * The real equation is device + kernel + SELinux + boot + provider.
 * [DeviceProfile] captures the device side of that equation so the
 * compatibility layer (Pixel / Xiaomi / Samsung / OPPO / vivo) and the
 * capability resolver can reason per-device.
 */
@Serializable
data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val androidVersion: Int,
    val kernelVersion: String,
    val selinuxMode: String,
) {
    companion object {
        fun empty(): DeviceProfile = DeviceProfile(
            manufacturer = "",
            model = "",
            androidVersion = 0,
            kernelVersion = "",
            selinuxMode = "",
        )
    }
}
