package com.astraveil.modules.model

import kotlinx.serialization.Serializable

/**
 * CANONICAL module manifest (v3). Single source of truth for module
 * metadata across the preview/inspector and the installer.
 *
 * P2-20 (model convergence): this type unifies the two existing manifest
 * representations — the Phase-0 `modules.ModuleManifest` (permissions as
 * `List<String>`, plus runtime/entry/minApi) and the v3
 * `modules.api.ModuleManifest` (structured `List<ModulePermission>`) —
 * so both the preview and the install pipeline consume ONE model.
 * This eliminates P1-7 ("preview succeeds, install fails") at the model
 * level: there is no longer a second representation to drift from.
 *
 * Existing code continues to compile unchanged; new install-path code
 * should convert to this canonical type via [legacyToCanonical] and
 * consume it uniformly.
 */
@Serializable
data class ModulePermission(
    val capability: String,
    val reason: String = "",
    /** Nullable/unknown risk is allowed — rendered as "Unknown", never fabricated. */
    val riskLevel: Int? = null,
)

@Serializable
data class ModuleManifest(
    val id: String,
    val name: String,
    val version: String,
    val apiVersion: Int = 3,
    val description: String = "",
    val author: String = "",
    val permissions: List<ModulePermission> = emptyList(),
    val requiredCapabilities: List<String> = emptyList(),
    val optionalCapabilities: List<String> = emptyList(),
    /** Native runtime path (e.g. "runtime/arm64.so"); blank = pure Kotlin. */
    val runtime: String = "",
    /** C-exported entry symbol; blank = JNI_OnLoad self-registration. */
    val entry: String = "",
    val minApi: Int = 1,
) {
    /** Backward-compat for Phase-0 code that expects `List<String>`. */
    val permissionNames: List<String>
        get() = permissions.map { it.capability }
}
