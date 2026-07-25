package com.astraveil.core.permission

import kotlinx.serialization.Serializable

/**
 * Tiered privilege levels understood by the AstraVeil permission engine.
 *
 * Higher [level] values denote more powerful privilege tiers; modules must
 * hold a tier at least as high as the operation they intend to perform.
 * The enum is serializable so it can be carried in module manifests and
 * persisted permission sets.
 *
 * @property level Numeric ordering of the tier.
 */
@Serializable
enum class Permission(val level: Int) {
    /** No elevated privileges. */
    NONE(0),

    /** Unprivileged shell (uid 2000). */
    SHELL(10),

    /** Root (uid 0). */
    ROOT(100),

    /** Kernel-level hooks / direct syscall interposition. */
    KERNEL(1000);

    companion object {
        /**
         * Look up a [Permission] by case-insensitive name.
         *
         * @return The matching permission, or `null` if [name] is not recognized.
         */
        fun fromName(name: String): Permission? =
            values().firstOrNull { it.name.equals(name, true) }
    }
}

/**
 * Serializable set of permission names granted to a single module.
 *
 * Permission names are free-form strings so providers can introduce new
 * capabilities without altering the core schema; [Permission] merely provides
 * a baseline tier vocabulary. The set is immutable — every mutation returns
 * a new instance so it can be safely shared across coroutines.
 *
 * @property permissions Granted permission names.
 */
@Serializable
data class PermissionSet(
    val permissions: Set<String> = emptySet(),
) {
    /**
     * Return a new [PermissionSet] with [name] added.
     */
    fun grant(name: String): PermissionSet = copy(permissions = permissions + name)

    /**
     * Return a new [PermissionSet] with [name] removed.
     */
    fun revoke(name: String): PermissionSet = copy(permissions = permissions - name)

    /**
     * Return `true` if [name] is in the set.
     */
    fun has(name: String): Boolean = name in permissions

    /**
     * Operator form of [has] enabling `name in permissionSet`.
     */
    operator fun contains(name: String): Boolean = name in permissions
}
