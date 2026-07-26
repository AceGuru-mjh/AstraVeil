package com.astraveil.providers

import kotlinx.serialization.Serializable

/**
 * v3 execution request.
 *
 * Replaces the bare `execute(command: String)` call with a structured
 * request carrying the originating module, the capability it claims to
 * exercise, and the command. This lets the permission engine + risk
 * engine + audit log attribute every root operation to a source.
 *
 * Added alongside the existing `RootProvider.execute(command)` so the
 * v2 path keeps working during migration; providers gain an
 * `execute(request: ExecutionRequest)` overload.
 *
 * @property moduleId   the AVM module requesting execution
 * @property capability  the capability token the module claims (e.g. "ROOT_ACCESS")
 * @property command     the shell command to run
 */
@Serializable
data class ExecutionRequest(
    val moduleId: String,
    val capability: String,
    val command: String,
)
