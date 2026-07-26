package com.astraveil.providers.runtime

import com.astraveil.providers.RootProvider

/**
 * Orchestrates provider selection + command execution.
 *
 * The daemon holds one [ProviderRuntime]; callers ask [execute] and the
 * runtime routes to the first available provider via [ProviderRegistry].
 */
class ProviderRuntime(
    private val registry: com.astraveil.providers.ProviderRegistry,
) {

    /** @return the active provider's [CommandExecutor], or null if no provider. */
    suspend fun executor(): CommandExecutor? {
        val provider = registry.detectActive()?.let {
            registry.byId(it.providerName)
        } ?: return null
        if (!provider.available()) return null
        return RootCommandExecutor(provider)
    }

    /** Execute @p command through the active provider. */
    suspend fun execute(command: String): com.astraveil.providers.ExecutionResult {
        val exec = executor() ?: return com.astraveil.providers.ExecutionResult(
            success = false,
            output = "",
            error = "no root provider available",
        )
        return exec.execute(command)
    }
}
