package com.astraveil.core.capability.detector

import com.astraveil.core.capability.CapabilityNode

/** Device-side probes (Android version, ABI, model). */
class DeviceCapabilityDetector : CapabilityDetector {
    override fun detect(): List<CapabilityNode> = listOf(
        CapabilityNode("android", true, "device", 100),
    )
}
