package com.astraveil.providers.runtime

import com.astraveil.providers.ExecutionResult

/**
 * Shell command executor abstraction.
 *
 * Modules and the daemon talk to root backends through this interface
 * so the execution path is testable without a live provider.
 */
interface CommandExecutor {
    suspend fun execute(command: String): ExecutionResult
}
