package com.astraveil.core.modules.model

/**
 * UI-facing module representation.
 *
 * This is NOT a copy of [com.astraveil.modules.ModuleManifest] or
 * [com.astraveil.modules.AstraModule]. It is a projection that the
 * Compose layer consumes. The adapter lives in the :app repository
 * layer so :core never depends on :modules.
 *
 * Risk data model (Patch 18.2.1):
 *  - [ModulePermissionInfo.risk] is **nullable**. A non-null value is
 *    ALWAYS sourced from the module manifest (v3 `riskLevel` field).
 *    A null value means the risk was not declared — the UI MUST render
 *    this as "Unknown", never as a guessed number.
 *  - The UI layer no longer runs heuristics (`defaultRisk` /
 *    `estimateRisk`) to fabricate a score. "显示真实能力，而不是模拟".
 */
data class ModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val state: ModuleUiState,
    val permissions: List<ModulePermissionInfo>,
)

/**
 * Simplified state for the UI. Mapped from the Phase-0
 * [com.astraveil.modules.ModuleState] by the repository adapter.
 */
enum class ModuleUiState {
    INSTALLED,
    RUNNING,
    STOPPED,
    FAILED,
}

/**
 * A single permission entry shown in the install-confirmation dialog
 * and the module detail card.
 *
 * @property risk Risk score declared in the module manifest (v3
 *                `riskLevel`), or `null` when the manifest did not
 *                declare one. `null` MUST be rendered as "Unknown" —
 *                the UI never invents a value.
 * @property reason Human-readable rationale declared in the manifest,
 *                  or empty string when absent.
 */
data class ModulePermissionInfo(
    val capability: String,
    val risk: Int?,
    val reason: String = "",
)
