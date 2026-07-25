package com.astraveil.core.permission

import com.astraveil.core.event.AstraEvent
import com.astraveil.core.event.EventBus
import com.astraveil.core.event.PermissionGrantedEvent
import com.astraveil.core.event.PermissionRevokedEvent
import com.astraveil.core.logger.AstraLogger

/**
 * Centralized permission broker for AstraVeil modules.
 *
 * Each module is identified by a string id and is associated with a
 * [PermissionSet]. Permission grants are subject to a small policy:
 *
 *  - **Dangerous permissions** (see [dangerousPermissions]) require explicit
 *    user approval, signalled via [setDangerousApproval]. While approval is
 *    withheld, [request] refuses to grant dangerous permissions and returns
 *    `false`.
 *  - Ordinary permissions can be granted freely via [request] or [grant].
 *
 * All mutations emit events through the supplied [EventBus] so that the UI
 * and other observers can react. The engine is safe to call from any thread;
 * its internal map is guarded by an intrinsic lock.
 *
 * @param eventBus Bus used to publish permission lifecycle events.
 */
class PermissionEngine(private val eventBus: EventBus) {

    companion object {
        /**
         * Module id used by callers that are not themselves a loadable .avm
         * module — e.g. the public SDK facade, the AstraUI shell, or built-in
         * tooling. Single-arg overloads below attribute their grants to this id
         * so the rest of the engine keeps working with a consistent key.
         */
        const val BUILTIN_MODULE_ID: String = "astra:builtin"
    }

    /**
     * Permission names that require explicit user approval before being
     * granted. The set is intentionally small and stable; new dangerous
     * capabilities should be added here so the policy gate picks them up.
     */
    private val dangerousPermissions: Set<String> = setOf(
        "mount",
        "su",
        "kernel_hook",
        "namespace",
    )

    private val granted: MutableMap<String, PermissionSet> = mutableMapOf()
    private val lock = Any()

    @Volatile
    private var dangerousApproval: Boolean = false

    /**
     * Attempt to grant [permission] to [moduleId] subject to the
     * dangerous-permission policy.
     *
     * @return `true` if the permission is now granted, `false` if policy
     *         refused it (e.g. a dangerous permission was requested while
     *         approval is withheld).
     */
    fun request(moduleId: String, permission: String): Boolean {
        if (permission in dangerousPermissions && !dangerousApproval) {
            AstraLogger.w(
                "PermissionEngine",
                "Refusing dangerous permission '$permission' for module '$moduleId' (no approval).",
            )
            return false
        }
        grant(moduleId, permission)
        return true
    }

    /**
     * Single-arg convenience overload for callers that are not a loadable
     * module (SDK facade, AstraUI shell, built-in tools). Equivalent to
     * `request([BUILTIN_MODULE_ID], permission)`.
     */
    fun request(permission: String): Boolean = request(BUILTIN_MODULE_ID, permission)

    /**
     * Grant [permission] to [moduleId] unconditionally, skipping the
     * dangerous-approval gate.
     *
     * Intended for callers that have already verified intent (e.g. the UI
     * after explicitly prompting the user, or a policy rule that allows the
     * grant). No-op if the module already holds the permission.
     */
    fun grant(moduleId: String, permission: String) {
        val updated: PermissionSet
        synchronized(lock) {
            val current = granted[moduleId] ?: PermissionSet()
            if (permission in current) return
            updated = current.grant(permission)
            granted[moduleId] = updated
        }
        AstraLogger.i("PermissionEngine", "Granted '$permission' to '$moduleId'.")
        emit(PermissionGrantedEvent(moduleId, permission))
    }

    /**
     * Revoke [permission] from [moduleId] if present. No-op otherwise.
     */
    fun revoke(moduleId: String, permission: String) {
        val removed: Boolean
        synchronized(lock) {
            val current = granted[moduleId] ?: return
            if (permission !in current) return
            granted[moduleId] = current.revoke(permission)
            removed = true
        }
        if (removed) {
            AstraLogger.i("PermissionEngine", "Revoked '$permission' from '$moduleId'.")
            emit(PermissionRevokedEvent(moduleId, permission))
        }
    }

    /**
     * Remove all permissions held by [moduleId]. Emits a
     * [PermissionRevokedEvent] for every previously held permission.
     */
    fun revokeAll(moduleId: String) {
        val revoked: PermissionSet
        synchronized(lock) {
            revoked = granted.remove(moduleId) ?: return
        }
        revoked.permissions.forEach { permission ->
            emit(PermissionRevokedEvent(moduleId, permission))
        }
        AstraLogger.i("PermissionEngine", "Revoked all permissions from '$moduleId' (${revoked.permissions.size} entries).")
    }

    /**
     * Toggle the master dangerous-permission approval flag.
     *
     * When set to `true`, [request] will grant dangerous permissions; when
     * `false`, such requests are refused. Typically wired to the user-facing
     * "developer mode" toggle.
     */
    fun setDangerousApproval(approved: Boolean) {
        dangerousApproval = approved
        AstraLogger.i("PermissionEngine", "Dangerous approval set to $approved.")
    }

    /**
     * Return a defensive snapshot of the [PermissionSet] currently held by
     * [moduleId]. Returns an empty set for unknown modules.
     */
    fun list(moduleId: String): PermissionSet = synchronized(lock) {
        granted[moduleId] ?: PermissionSet()
    }

    /**
     * Return `true` if [moduleId] currently holds [permission].
     */
    fun canExecute(moduleId: String, permission: String): Boolean =
        permission in list(moduleId)

    /**
     * Single-arg convenience overload: `true` if the built-in caller currently
     * holds [permission]. Equivalent to `canExecute([BUILTIN_MODULE_ID], permission)`.
     */
    fun has(permission: String): Boolean = canExecute(BUILTIN_MODULE_ID, permission)

    /**
     * Publish [event] on the bus using the non-suspending [EventBus.tryEmit]
     * so callers are not required to be inside a coroutine.
     */
    private fun emit(event: AstraEvent) {
        eventBus.tryEmit(event)
    }
}
