package com.astraveil.core.security.audit

import kotlinx.serialization.Serializable

/**
 * One audit-trail entry recording a security decision.
 *
 * Written by [AuditLogger.record] to `/data/astra/log/security.log`
 * (one JSON object per line) so the AstraUI Security panel can tail
 * and inspect it.
 */
@Serializable
data class AuditEntry(
    val requestId: String,
    val moduleId: String,
    val capability: String,
    val decision: String,
    val timestamp: Long,
)
