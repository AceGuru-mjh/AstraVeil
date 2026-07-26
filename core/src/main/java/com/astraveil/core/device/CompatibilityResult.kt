package com.astraveil.core.device

import kotlinx.serialization.Serializable

/**
 * Result of running [CompatibilityChecker.check] against an
 * [AndroidProfile]. AstraVeil targets Android 10 (SDK 29) through
 * Android 16 (SDK 36); anything older is [supported] = false.
 */
@Serializable
data class CompatibilityResult(
    val supported: Boolean,
    val warnings: List<String>,
)
