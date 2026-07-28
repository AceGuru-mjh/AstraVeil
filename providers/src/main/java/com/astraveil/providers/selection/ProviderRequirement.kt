package com.astraveil.providers.selection

import com.astraveil.providers.ProviderCapability

/**
 * Describes what a module or the daemon needs from a root provider.
 *
 * Instead of asking for "Magisk" or "KernelSU", the caller asks for a
 * capability ("overlayfs", "namespace", "execute") with a minimum
 * confidence score. The [ProviderSelector] then picks the best backend
 * that satisfies the requirement.
 *
 * @property capability      the capability token needed
 * @property minScore        minimum acceptable score (0–100)
 * @property allowFallback   if true, return the best available even if
 *                           below [minScore]; if false, return null
 * @property preferredProvider hint for the selector (e.g. "kernelsu")
 */
data class ProviderRequirement(
    val capability: ProviderCapability,
    val minScore: Int = 50,
    val allowFallback: Boolean = true,
    val preferredProvider: String? = null,
) {
    companion object {
        fun execute(minScore: Int = 50) = ProviderRequirement(
            capability = ProviderCapability.ROOT_EXECUTION,
            minScore = minScore,
        )
        fun mount(minScore: Int = 50) = ProviderRequirement(
            capability = ProviderCapability.MOUNT_NAMESPACE,
            minScore = minScore,
        )
        fun overlayFs(minScore: Int = 50) = ProviderRequirement(
            capability = ProviderCapability.OVERLAY_FS,
            minScore = minScore,
        )
    }
}
