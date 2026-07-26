package com.astraveil.core.security.audit

/**
 * Appends [AuditEntry]s to the security audit log.
 *
 * Phase 2.3 Kotlin-side logger. The daemon already has its own C++
 * AuditLogger (`daemon/src/security/audit_logger.cpp`) writing to the
 * same file; this Kotlin logger lets the AstraCore permission engine
 * record its decisions without an IPC round-trip.
 */
class AuditLogger {

    /** Record one audit entry. Phase 2.3: stdout; file append TODO. */
    fun record(entry: AuditEntry) {
        // TODO: append to /data/astra/log/security.log as JSON-per-line.
        println("AUDIT ${entry.timestamp} ${entry.moduleId} ${entry.capability} ${entry.decision}")
    }
}
