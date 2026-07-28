package com.astraveil.providers.selection

import com.astraveil.providers.ProviderCapability
import com.astraveil.providers.ProviderReport

/**
 * Selects the best provider for a given [ProviderRequirement] from a
 * list of [ProviderReport]s.
 *
 * Selection algorithm:
 *  1. Filter reports to those that are [ProviderReport.detected].
 *  2. Extract the per-capability score for the requested capability.
 *  3. Filter out providers whose score is 0 (capability not offered).
 *  4. If a [ProviderRequirement.preferredProvider] is set and it
 *     qualifies, prefer it.
 *  5. Otherwise pick the highest score.
 *  6. If the best score is below [ProviderRequirement.minScore]:
 *     - return null if [ProviderRequirement.allowFallback] is false
 *     - return the best with fallback=true otherwise
 */
class ProviderSelector {

    fun select(
        requirement: ProviderRequirement,
        reports: List<ProviderReport>,
    ): ProviderSelectionResult? {
        val detected = reports.filter { it.detected }
        if (detected.isEmpty()) return null

        val scored = detected.map { report ->
            val score = scoreFor(report, requirement.capability)
            report to score
        }.filter { it.second > 0 }

        if (scored.isEmpty()) return null

        // Check preferred provider first
        val preferred = requirement.preferredProvider
        if (preferred != null) {
            val match = scored.firstOrNull { it.first.providerId == preferred }
            if (match != null && match.second >= requirement.minScore) {
                return ProviderSelectionResult(
                    providerId = match.first.providerId,
                    score = match.second,
                    confidence = match.second / 100f,
                    reason = "Preferred provider '${preferred}' qualifies",
                )
            }
        }

        // Pick highest score
        val best = scored.maxByOrNull { it.second } ?: return null
        val isFallback = best.second < requirement.minScore

        if (isFallback && !requirement.allowFallback) return null

        return ProviderSelectionResult(
            providerId = best.first.providerId,
            score = best.second,
            confidence = best.second / 100f,
            reason = if (isFallback) "Best available (below minimum)" else "Highest capability score",
            fallback = isFallback,
        )
    }

    private fun scoreFor(
        report: ProviderReport,
        capability: ProviderCapability,
    ): Int = when (capability) {
        ProviderCapability.ROOT_EXECUTION -> report.executeScore
        ProviderCapability.MOUNT_NAMESPACE -> report.mountScore
        ProviderCapability.OVERLAY_FS -> report.overlayFsScore
        ProviderCapability.SYSTEM_PROPERTY -> report.propertyScore
        ProviderCapability.BOOT_PATCH -> report.bootScore
        ProviderCapability.SELINUX_CONTROL -> report.overallScore
    }
}
