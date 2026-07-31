package com.astraveil.app.execution

import com.astraveil.core.execution.CommandAuditEntry
import com.astraveil.core.execution.CommandAuditLogger
import com.astraveil.core.execution.SessionEvent
import com.astraveil.core.execution.SessionSource
import com.astraveil.providers.ProviderExecResult
import com.astraveil.providers.RootProvider
import java.util.UUID

/**
 * The ONLY legitimate path for raw privileged command execution.
 *
 * Used by human-driven features (Terminal, Root Test). Enforces:
 *   1. Explicit user approval before any command runs
 *   2. Full audit logging of every command
 *   3. A bounded session that must be re-approved if reopened
 *
 * MODULE EXECUTION MUST NEVER USE THIS. Modules go through the
 * capability broker + Rust policy. This class is for interactive,
 * user-typed commands only.
 *
 * Lives in `:app` (not `:core`) because it references [RootProvider]
 * from `:providers`, which `:core` cannot depend on. The audit sink
 * [CommandAuditLogger] stays in `:core` so any future consumer can
 * reuse the persistence format.
 *
 * @param provider      the active root backend that will actually run commands
 * @param auditLogger   append-only audit sink
 * @param source        which feature opened this session (TERMINAL / ROOT_TEST / …)
 */
class TrustedInteractiveSession(
    private val provider: RootProvider,
    private val auditLogger: CommandAuditLogger,
    val source: SessionSource,
) {
    val sessionId: String = UUID.randomUUID().toString()

    @Volatile
    private var approved = false

    @Volatile
    private var closed = false

    /**
     * Record the user's explicit acknowledgment that commands here run
     * with elevated privilege and are their responsibility. Must be
     * called before [execute]. Idempotent within a session.
     */
    fun approve() {
        check(!closed) { "session closed" }
        if (approved) return
        approved = true
        auditLogger.logSession(
            SessionEvent(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                source = source.name,
                event = "APPROVED",
            )
        )
    }

    fun isApproved(): Boolean = approved && !closed

    /**
     * Execute a command. Refuses unless the session is approved.
     * Every execution is audited.
     *
     * The [timeoutMs] parameter is accepted for API completeness and
     * recorded in the session contract; the underlying [RootProvider]
     * does not currently honour a per-call timeout, so [ProviderExecResult.timedOut]
     * is surfaced unchanged. When a provider gains timeout support it
     * will be wired through here without an API change.
     *
     * @throws IllegalStateException if not approved
     */
    suspend fun execute(command: String, timeoutMs: Long = 30_000): ProviderExecResult {
        check(isApproved()) {
            "Interactive session not approved — refusing to execute"
        }

        val result = provider.execute(command)

        auditLogger.logCommand(
            CommandAuditEntry(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                source = source.name,
                backend = provider.id,
                command = command,
                exitCode = if (result.success) 0 else 1,
                success = result.success,
                timedOut = result.timedOut,
                outputPreview = (result.stdout.ifBlank { result.stderr })
                    .take(500),
            )
        )
        return result
    }

    /** Close the session. Further execution requires a new session. */
    fun close() {
        if (closed) return
        closed = true
        approved = false
        auditLogger.logSession(
            SessionEvent(
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                source = source.name,
                event = "CLOSED",
            )
        )
    }
}
