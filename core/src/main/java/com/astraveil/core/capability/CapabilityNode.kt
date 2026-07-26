package com.astraveil.core.capability

import kotlinx.serialization.Serializable

/**
 * One node in the v3 capability graph.
 *
 * Each capability is reported with:
 * @property name        the capability token ("overlayfs", "mount_namespace", ...)
 * @property available   whether it is currently usable
 * @property source      where the claim comes from ("kernel" / "provider" / "device" / "selinux" / "boot")
 * @property confidence  0–100 trust in the claim
 */
@Serializable
data class CapabilityNode(
    val name: String,
    val available: Boolean,
    val source: String,
    val confidence: Int,
)
