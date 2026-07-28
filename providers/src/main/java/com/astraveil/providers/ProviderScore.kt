package com.astraveil.providers

/**
 * v3 provider selection score with strengths and limitations.
 *
 * Score components:
 *   capability match   +20  (provider advertises the requested capability)
 *   device compat      +20  (provider known-good on this device profile)
 *   security level     +30  (AstraRoot > KernelSU > APatch > Magisk)
 *   stability          +30  (provider has not crashed recently)
 */
data class ProviderScore(
    val providerId: String,
    val score: Int,
    val strengths: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
)
