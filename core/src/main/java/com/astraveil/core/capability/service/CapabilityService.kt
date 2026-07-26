package com.astraveil.core.capability.service

import com.astraveil.core.capability.CapabilityResolverV2

/**
 * Public query API over the resolved capability graph.
 *
 * Callers ask `supports("overlayfs")` instead of holding a reference to
 * the resolver + graph; this service hides the resolve-then-query
 * pattern and is the entry point AstraUI and the SDK use.
 */
class CapabilityService(
    private val resolver: CapabilityResolverV2,
) {

    /** True iff the resolved graph contains an available @p name node. */
    suspend fun supports(name: String): Boolean =
        resolver.resolve().has(name)
}
