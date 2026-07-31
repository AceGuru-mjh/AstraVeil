package com.astraveil.modules.compatibility

data class CompatibilityReport(
    val compatible: Boolean,
    val satisfied: List<String>,
    val missing: List<String>,
    val optionalMissing: List<String>,
) {
    val summary: String get() = if (compatible) "Compatible with this device." else "Missing required: ${missing.joinToString(", ")}"
}

object CapabilityCompatibilityChecker {
    fun check(required: List<String>, optional: List<String>, deviceMatrix: Map<String, Boolean>): CompatibilityReport {
        val satisfied = required.filter { deviceMatrix[it] == true }
        val missing = required.filter { deviceMatrix[it] != true }
        val optionalMissing = optional.filter { deviceMatrix[it] != true }
        return CompatibilityReport(missing.isEmpty(), satisfied, missing, optionalMissing)
    }
}
