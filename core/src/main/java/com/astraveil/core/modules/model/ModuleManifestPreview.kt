package com.astraveil.core.modules.model

/**
 * Core-level projection of a parsed `.avm` manifest, BEFORE any UI
 * adaptation. Produced by [com.astraveil.core.modules.manifest.AvmManifestParser]
 * and consumed by:
 *  - [com.astraveil.core.modules.security.ModuleScanner] (to build a
 *    [com.astraveil.core.modules.security.TrustReport])
 *  - The `:app` repository adapter (to build the UI-facing `ModuleInfo`)
 *
 * This type deliberately lives in `:core` and has NO Compose / Android
 * dependency, so the daemon, Rust FFI, CLI, and tests can all reuse the
 * same manifest projection without dragging in UI types.
 *
 * Risk model (Patch 18.2.1, carried into PR18.3):
 *  - [PermissionDeclaration.risk] is **nullable**. A non-null value is
 *    ALWAYS sourced from the manifest (v3 `riskLevel`). `null` means
 *    the manifest did not declare a risk (Phase-0 string-only format).
 *  - The parser runs NO heuristics. "显示真实能力，而不是模拟".
 *
 * PR18.3 note: this type replaces the earlier (wrong) practice of
 * returning `ModuleInfo` directly from the parser. `ModuleInfo` remains
 * the UI-facing projection, produced only by the `:app` adapter.
 */
data class ModuleManifestPreview(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val permissions: List<PermissionDeclaration>,
)

/**
 * A single permission declaration extracted from a module manifest.
 *
 * @property capability Permission token (e.g. "filesystem", "mount",
 *                      "root_execution"). For Phase-0 manifests this is
 *                      the raw string from the `permissions` array; for
 *                      v3 manifests it is the `capability` field.
 * @property risk       Risk score declared in the manifest (v3
 *                      `riskLevel`), or `null` when the manifest did
 *                      not declare one (Phase-0). `null` MUST be
 *                      rendered as "Unknown" — never a fabricated value.
 * @property reason     Human-readable rationale declared in the manifest
 *                      (v3 `reason`), or empty string when absent.
 */
data class PermissionDeclaration(
    val capability: String,
    val risk: Int?,
    val reason: String = "",
)
