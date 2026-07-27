package com.astraveil.core.compatibility

enum class CompatibilityLevel {
    EXCELLENT,
    GOOD,
    LIMITED,
    UNSUPPORTED,
}

data class CompatibilityResult(
    val level: CompatibilityLevel,
    val score: Int,
    val warnings: List<String>,
    val blockedCapabilities: List<String>,
) {
    companion object {
        fun unknown() = CompatibilityResult(CompatibilityLevel.UNSUPPORTED, 0, emptyList(), emptyList())
    }
}
