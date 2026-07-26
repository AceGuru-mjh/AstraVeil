package com.astraveil.modules.api

import kotlinx.serialization.Serializable

/**
 * v3 AVM module manifest.
 *
 * Example manifest.json:
 * @code
 * {
 *   "id": "example.module",
 *   "name": "Example Module",
 *   "version": "1.0.0",
 *   "apiVersion": 2,
 *   "permissions": [
 *     { "capability": "mount_namespace", "reason": "overlay mount", "riskLevel": 70 }
 *   ]
 * }
 * @endcode
 */
@Serializable
data class ModuleManifest(
    val id: String,
    val name: String,
    val version: String,
    val apiVersion: Int,
    val permissions: List<ModulePermission>,
)
