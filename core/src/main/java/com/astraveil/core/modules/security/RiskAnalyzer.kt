package com.astraveil.core.modules.security

import com.astraveil.core.modules.model.ModuleManifestPreview

/**
 * Derives an overall risk assessment from a parsed module manifest
 * (PR18.3).
 *
 * The analyzer runs NO heuristics and invents NO risk values. It only
 * aggregates what the manifest actually declared:
 *  - If every permission has `risk == null` (Phase-0 string-only
 *    format) → [RiskSource.UNDECLARED], [RiskLevel.UNKNOWN].
 *  - If at least one permission declared a `riskLevel` (v3) →
 *    [RiskSource.MANIFEST], [RiskLevel] derived from the max.
 *
 * Future (post-MVP, per PR18.3 design review): a richer model
 * combining `capability + target + operation` (e.g. `filesystem`
 * writing to `/system` is high-risk; writing to `/data/local/tmp`
 * is low-risk). That requires v4 manifest support and is out of
 * scope for this PR.
 *
 * Pure JVM, stateless, thread-safe.
 */
object RiskAnalyzer {

    /**
     * @param preview The parsed manifest, or `null` when the manifest
     *                could not be parsed. `null` produces a [NONE]
     *                assessment with [RiskLevel.UNKNOWN].
     */
    fun analyze(preview: ModuleManifestPreview?): Assessment {
        if (preview == null) {
            return Assessment(
                permissionCount = 0,
                highestRisk = null,
                riskSource = RiskSource.NONE,
                overallRiskLevel = RiskLevel.UNKNOWN,
            )
        }

        val permissions = preview.permissions
        if (permissions.isEmpty()) {
            return Assessment(
                permissionCount = 0,
                highestRisk = null,
                riskSource = RiskSource.NONE,
                overallRiskLevel = RiskLevel.UNKNOWN,
            )
        }

        val declaredRisks = permissions.mapNotNull { it.risk }
        return if (declaredRisks.isEmpty()) {
            // Phase-0: no risk declared at all.
            Assessment(
                permissionCount = permissions.size,
                highestRisk = null,
                riskSource = RiskSource.UNDECLARED,
                overallRiskLevel = RiskLevel.UNKNOWN,
            )
        } else {
            // v3: at least one declared risk. Use the max.
            val maxRisk = declaredRisks.max()
            Assessment(
                permissionCount = permissions.size,
                highestRisk = maxRisk,
                riskSource = RiskSource.MANIFEST,
                overallRiskLevel = levelFor(maxRisk),
            )
        }
    }

    /** Map a numeric risk score to a [RiskLevel]. `null` → UNKNOWN. */
    fun levelFor(risk: Int?): RiskLevel = when (risk) {
        null -> RiskLevel.UNKNOWN
        else -> when {
            risk <= 30 -> RiskLevel.LOW
            risk <= 70 -> RiskLevel.MEDIUM
            risk <= 89 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }
    }

    /**
     * Aggregated risk metrics for a single manifest.
     *
     * Folded into [TrustReport] by [ModuleScanner].
     */
    data class Assessment(
        val permissionCount: Int,
        val highestRisk: Int?,
        val riskSource: RiskSource,
        val overallRiskLevel: RiskLevel,
    )
}
