package com.astraveil.providers

import kotlinx.serialization.Serializable

/**
 * Detailed per-provider capability report with confidence scores.
 *
 * Produced by [com.astraveil.providers.intelligence.ProviderAnalyzer]
 * from a RootProvider's advertised capabilities + the device profile.
 *
 * @property providerId      e.g. "magisk", "kernelsu"
 * @property displayName     e.g. "Magisk", "KernelSU"
 * @property version         version string or "unknown"
 * @property detected        true if the provider is available on this device
 * @property executeScore    0–100 confidence for root execution
 * @property mountScore      0–100 confidence for mount operations
 * @property namespaceScore  0–100 confidence for namespace isolation
 * @property overlayFsScore  0–100 confidence for OverlayFS
 * @property propertyScore   0–100 confidence for property modification
 * @property bootScore       0–100 confidence for boot integration
 * @property overallScore    weighted average of all capability scores
 * @property strengths       list of capabilities the provider excels at
 * @property limitations     list of known limitations on this device
 */
@Serializable
data class ProviderReport(
    val providerId: String,
    val displayName: String,
    val version: String = "unknown",
    val detected: Boolean = false,
    val executeScore: Int = 0,
    val mountScore: Int = 0,
    val namespaceScore: Int = 0,
    val overlayFsScore: Int = 0,
    val propertyScore: Int = 0,
    val bootScore: Int = 0,
    val overallScore: Int = 0,
    val strengths: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
) {
    companion object {
        fun empty(providerId: String) = ProviderReport(
            providerId = providerId,
            displayName = providerId,
        )
    }
}
