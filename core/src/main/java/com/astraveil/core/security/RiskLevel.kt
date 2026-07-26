package com.astraveil.core.security

/**
 * Risk levels used by the risk engine and the sandbox policy resolver.
 *
 * The numeric [value] is the baseline score for a module declaring this
 * level; the final score is computed by summing per-permission
 * contributions (see [com.astraveil.core.security.audit.RiskEngine]).
 */
enum class RiskLevel(val value: Int) {
    LOW(10),
    MEDIUM(40),
    HIGH(70),
    CRITICAL(95),
}
