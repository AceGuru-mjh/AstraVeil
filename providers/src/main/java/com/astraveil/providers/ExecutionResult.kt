package com.astraveil.providers

import kotlinx.serialization.Serializable

/**
 * v3 execution result — the structured return of
 * `RootProvider.execute(request: ExecutionRequest)`.
 *
 * Carries success, captured output, and an optional error message so
 * the audit log and the UI can render the outcome without re-parsing
 * stdout.
 */
@Serializable
data class ExecutionResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
) {
    companion object {
        fun denied(reason: String): ExecutionResult =
            ExecutionResult(success = false, output = "", error = reason)
    }
}
