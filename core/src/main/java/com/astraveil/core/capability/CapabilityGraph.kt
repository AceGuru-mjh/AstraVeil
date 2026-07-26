package com.astraveil.core.capability

import kotlinx.serialization.Serializable

/**
 * v3 capability graph — the merged output of every [CapabilityDetector].
 *
 * Replaces the flat [CapabilityMatrix] as the rich, attributed view of
 * what the device can do. Each node carries its source + confidence so
 * the UI can show "overlayfs: available (kernel, 95%)" instead of a
 * bare boolean.
 */
@Serializable
data class CapabilityGraph(
    val nodes: List<CapabilityNode>,
) {
    /** True iff any node with @p name is available. */
    fun has(capability: String): Boolean =
        nodes.any { it.name == capability && it.available }

    /** All nodes for @p capability (there may be several from different sources). */
    fun get(capability: String): List<CapabilityNode> =
        nodes.filter { it.name == capability }

    companion object {
        fun empty(): CapabilityGraph = CapabilityGraph(emptyList())
    }
}
