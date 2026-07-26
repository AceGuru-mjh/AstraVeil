package com.astraveil.modules.api

import kotlinx.serialization.Serializable

/**
 * v3 module permission declaration.
 *
 * A module does not request bare "filesystem" — it requests a
 * capability with a reason and a declared risk level. The permission
 * engine + risk engine + sandbox resolver consume all three fields.
 */
@Serializable
data class ModulePermission(
    val capability: String,
    val reason: String,
    val riskLevel: Int,
)
