package com.astraveil.providers.selection

/**
 * Result of a provider selection decision.
 *
 * @property providerId   the id of the selected provider
 * @property score        the capability score that won
 * @property confidence   score / 100f
 * @property reason       human-readable explanation
 * @property fallback     true if the selected provider was below the
 *                        minimum score (only when allowFallback = true)
 */
data class ProviderSelectionResult(
    val providerId: String,
    val score: Int,
    val confidence: Float,
    val reason: String,
    val fallback: Boolean = false,
)
