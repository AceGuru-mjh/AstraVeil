package com.astraveil.core.capability.detector

import com.astraveil.core.capability.CapabilityNode

/** Kernel-side probes (mount namespace, overlayfs, pid namespace). */
class KernelCapabilityDetector : CapabilityDetector {
    override fun detect(): List<CapabilityNode> = listOf(
        CapabilityNode("mount_namespace", true, "kernel", 90),
        CapabilityNode("pid_namespace", true, "kernel", 90),
    )
}
