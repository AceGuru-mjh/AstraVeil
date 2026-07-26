package com.astraveil.core.capability

import com.astraveil.core.capability.detector.CapabilityDetector

/**
 * v3.0 Capability Resolver v2 — merges every registered
 * [CapabilityDetector] into a single [CapabilityGraph].
 *
 * Replaces the flat [CapabilityMatrix] output of [CapabilityResolverImpl]
 * with a rich, attributed graph. Each detector contributes its source-
 * tagged nodes; the resolver flat-maps them into one graph.
 *
 * The v1 [CapabilityResolver] / [CapabilityResolverImpl] remain for
 * backward compatibility; this v2 is the canonical v3 surface.
 */
class CapabilityResolverV2(
    private val detectors: List<CapabilityDetector>,
) {

    /** Resolve the full capability graph from every detector. */
    suspend fun resolve(): CapabilityGraph {
        val nodes = detectors.flatMap { it.detect() }
        return CapabilityGraph(nodes)
    }
}
