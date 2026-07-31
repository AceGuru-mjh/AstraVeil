package com.astraveil.core.provenance

enum class DataProvenance(val label: String, val rank: Int) {
    PROBED("Verified", 4), DETECTED("Detected", 3), ADVERTISED("Reported", 2),
    INFERRED("Inferred", 1), UNAVAILABLE("Unavailable", 0);
    val confidence: Int get() = when (this) { PROBED -> 100; DETECTED -> 80; ADVERTISED -> 60; INFERRED -> 30; UNAVAILABLE -> 0 }
}

data class ProvenancedValue<T>(val value: T?, val provenance: DataProvenance, val source: String? = null) {
    val isVerified: Boolean get() = provenance == DataProvenance.PROBED
    val verifiedValue: T? get() = if (isVerified) value else null
    fun displayValue(fallback: String = "—"): String = value?.toString() ?: fallback
    companion object {
        fun <T> probed(value: T, source: String? = null) = ProvenancedValue(value, DataProvenance.PROBED, source)
        fun <T> detected(value: T, source: String? = null) = ProvenancedValue(value, DataProvenance.DETECTED, source)
        fun <T> advertised(value: T, source: String? = null) = ProvenancedValue(value, DataProvenance.ADVERTISED, source)
        fun <T> inferred(value: T, source: String? = null) = ProvenancedValue(value, DataProvenance.INFERRED, source)
        fun <T> unavailable(source: String? = null) = ProvenancedValue<T>(null, DataProvenance.UNAVAILABLE, source)
    }
}
