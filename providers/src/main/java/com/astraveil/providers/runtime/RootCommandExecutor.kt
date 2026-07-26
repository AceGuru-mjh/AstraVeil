package com.astraveil.providers.runtime

import com.astraveil.providers.ExecutionRequest
import com.astraveil.providers.ExecutionResult
import com.astraveil.providers.RootProvider

/**
 * [CommandExecutor] backed by a [RootProvider].
 *
 * Wraps every command in an [ExecutionRequest] tagged with
 * `root_execution` capability so the permission engine + Rust policy
 * can attribute and gate it.
 */
class RootCommandExecutor(
    private val provider: RootProvider,
) : CommandExecutor {

    override suspend fun execute(command: String): ExecutionResult {
        return provider.execute(
            ExecutionRequest(
                moduleId = "system",
                capability = "root_execution",
                command = command,
            )
        )
    }
}
