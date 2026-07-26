package com.astraveil.core.execution

import kotlinx.serialization.Serializable

/**
 * v3 execution context — carried by every root operation.
 *
 * Outlaws the bare `provider.execute("mount /system")` pattern: every
 * execution now has a request id, an originating module, the capability
 * it claims, the user's approval state, and a risk level. This is what
 * the audit log, the permission engine, and the risk engine read.
 *
 * @property requestId   correlation id for tracing / audit
 * @property moduleId    the AVM module requesting execution
 * @property capability   capability token claimed ("ROOT_ACCESS", ...)
 * @property approved     true iff the user (or cached policy) approved
 * @property riskLevel    0–100 risk score from the RiskEngine
 */
@Serializable
data class ExecutionContext(
    val requestId: String,
    val moduleId: String,
    val capability: String,
    val approved: Boolean,
    val riskLevel: Int,
)
