package com.astraveil.core.permission

import kotlinx.serialization.Serializable

/**
 * v3 permission request raised by a module.
 *
 * Replaces the implicit `check(permission)` call with a structured
 * request carrying the module id, the capability token, and a
 * human-readable reason shown in the AstraUI permission dialog.
 *
 * @property moduleId   the AVM module requesting the permission
 * @property capability  capability token ("ROOT_ACCESS", "MOUNT", ...)
 * @property reason      rationale shown to the user
 */
@Serializable
data class PermissionRequest(
    val moduleId: String,
    val capability: String,
    val reason: String,
)
