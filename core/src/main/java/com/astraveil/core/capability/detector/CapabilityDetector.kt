package com.astraveil.core.capability.detector

import com.astraveil.core.capability.CapabilityNode

/**
 * One source of capability claims. The [CapabilityResolverImpl] flat-maps
 * every registered detector's [detect] output into the final graph.
 *
 * Implementations: [DeviceCapabilityDetector], [KernelCapabilityDetector],
 * [SelinuxCapabilityDetector], [ProviderCapabilityDetector], ...
 */
interface CapabilityDetector {
    fun detect(): List<CapabilityNode>
}
