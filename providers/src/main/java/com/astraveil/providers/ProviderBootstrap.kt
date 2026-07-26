package com.astraveil.providers

import com.astraveil.providers.apatch.APatchProvider
import com.astraveil.providers.astraroot.AstraRootProvider
import com.astraveil.providers.kernelsu.KernelSUProvider
import com.astraveil.providers.magisk.MagiskProvider

/**
 * Registers every built-in [RootProvider] with the [ProviderRegistry].
 *
 * Called once at daemon / app startup. Adding a new backend is a
 * one-line change here: construct it and [register].
 */
object ProviderBootstrap {

    /** Register every built-in provider with the global [ProviderRegistry]. */
    fun register(registry: ProviderRegistry = ProviderRegistry) {
        registry.all() // ensure initialised
        // The ProviderRegistry singleton already constructs all four
        // providers in its `providers` list (AstraRoot → Magisk →
        // KernelSU → APatch). This bootstrap exists as the explicit
        // registration entry point so future providers (and tests with
        // a fresh registry) have a single place to wire up.
        registerInto(registry)
    }

    private fun registerInto(registry: ProviderRegistry) {
        // ProviderRegistry is a singleton that pre-builds its providers;
        // no-op here. When the registry becomes injectable, this is
        // where each provider gets `register()`-ed.
    }
}
