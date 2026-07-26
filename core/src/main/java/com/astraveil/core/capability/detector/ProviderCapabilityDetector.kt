package com.astraveil.core.capability.detector

import com.astraveil.core.capability.CapabilityNode
import com.astraveil.providers.ProviderCapability
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.runBlocking

/**
 * Provider-side probe — asks the [ProviderRegistry] which capabilities
 * the active provider advertises and reports each as a graph node
 * sourced from that provider's id.
 */
class ProviderCapabilityDetector(
    private val registry: ProviderRegistry,
) : CapabilityDetector {

    override fun detect(): List<CapabilityNode> {
        val result = mutableListOf<CapabilityNode>()
        // runBlocking is acceptable here: ProviderRegistry.all() is
        // non-suspending and available() probes are fast file checks.
        runBlocking {
            for (provider in registry.all()) {
                if (!provider.available()) continue
                for (cap in provider.capabilities()) {
                    result.add(
                        CapabilityNode(
                            name = cap.name,
                            available = true,
                            source = provider.id,
                            confidence = 95,
                        )
                    )
                }
            }
        }
        return result
    }
}
