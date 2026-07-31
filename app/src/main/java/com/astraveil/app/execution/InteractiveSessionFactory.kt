package com.astraveil.app.execution

import com.astraveil.core.execution.CommandAuditLogger
import com.astraveil.core.execution.SessionSource
import com.astraveil.providers.ProviderRegistry
import com.astraveil.providers.RootProvider

/**
 * Creates gated interactive sessions against the active root backend.
 *
 * Centralizes session creation so every interactive feature goes through
 * the same approval + audit path. Callers receive an unapproved session
 * and MUST surface the approval UI before invoking [TrustedInteractiveSession.approve].
 */
class InteractiveSessionFactory(
    private val auditLogger: CommandAuditLogger,
) {
    /**
     * Open a session for [source]. The returned session is NOT yet
     * approved — the caller must surface the approval UI and call
     * [TrustedInteractiveSession.approve] after the user acknowledges.
     *
     * @return null if no functional root backend is present
     */
    suspend fun open(source: SessionSource): TrustedInteractiveSession? {
        val provider: RootProvider = ProviderRegistry.activeProvider() ?: return null
        return TrustedInteractiveSession(provider, auditLogger, source)
    }
}
