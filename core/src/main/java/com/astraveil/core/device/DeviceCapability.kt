package com.astraveil.core.device

import kotlinx.serialization.Serializable

/**
 * Device-side capability snapshot — what the hardware + kernel actually
 * allow, independent of any root provider.
 *
 * The [CapabilityResolver] merges this with provider capabilities to
 * produce the final [CapabilityMatrix] / [CapabilityGraph].
 */
@Serializable
data class DeviceCapability(
    val rootAvailable: Boolean = false,
    val mountNamespace: Boolean = false,
    val overlayFs: Boolean = false,
    val selinuxControl: Boolean = false,
    val bootPatch: Boolean = false,
)
