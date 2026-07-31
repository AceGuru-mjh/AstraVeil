package com.astraveil.app.diagnostics

import com.astraveil.core.diagnostics.SystemHealthStatus
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Produces a [SystemHealthStatus] snapshot for the Dashboard by probing
 * the [ProviderRegistry] for the active root backend.
 */
class MvpHealthCheck(
    private val registry: ProviderRegistry = ProviderRegistry,
) {
    suspend fun check(): SystemHealthStatus = withContext(Dispatchers.IO) {
        val info = registry.detectActive()
        val provider = info?.let { registry.byId(it.providerName) }
        val available = provider?.available() == true

        val caps = if (available) {
            provider!!.capabilities().map { it.name }
        } else {
            emptyList()
        }

        SystemHealthStatus(
            daemonOnline = available,
            provider = info?.displayName ?: "none",
            rootAvailable = available,
            capabilities = caps,
        )
    }
}
