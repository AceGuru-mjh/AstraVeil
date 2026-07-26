package com.astraveil.core.capability.detector

import com.astraveil.core.capability.CapabilityNode

/** SELinux probe — reports whether SELinux policy control is possible. */
class SelinuxCapabilityDetector : CapabilityDetector {
    override fun detect(): List<CapabilityNode> = listOf(
        // Phase 2.4 stub: real probe reads /sys/fs/selinux/enforce.
        CapabilityNode("selinux_control", false, "selinux", 80),
    )
}
