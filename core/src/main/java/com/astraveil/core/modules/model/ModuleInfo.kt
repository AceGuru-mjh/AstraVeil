package com.astraveil.core.modules.model

/**
 * UI-facing module representation.
 *
 * This is NOT a copy of [com.astraveil.modules.ModuleManifest] or
 * [com.astraveil.modules.AstraModule]. It is a projection that the
 * Compose layer consumes. The adapter lives in the :app repository
 * layer so :core never depends on :modules.
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
 */
data class ModulePermissionInfo(
    val capability: String,
    val risk: Int,
    val reason: String,
    /**
     * Where the [risk] value came from.
     *
     * - [RiskSource.MANIFEST]: the module author declared this risk level
     *   in `module.json` (v3 format). Trustworthy.
     * - [RiskSource.ESTIMATED]: the risk was inferred by heuristic because
     *   the manifest uses the Phase-0 string-only permission format.
     *   The UI must display this as an estimate, not a guarantee.
     */
    val riskSource: RiskSource = RiskSource.ESTIMATED,
)

/**
 * Provenance of a permission's risk score.
 */
enum class RiskSource {
    /** Risk level declared in the module's manifest (v3 format). */
    MANIFEST,

    /** Risk level inferred by heuristic (Phase-0 string permissions). */
    ESTIMATED,
}
