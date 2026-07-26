package com.astraveil.core.security

/**
 * v3 policy decision — mirrors the Rust [PolicyDecision] enum and the
 * C++ [PolicyResult] enum so all three layers speak the same vocabulary.
 */
enum class PolicyResult {
    ALLOW,
    DENY,
    REQUIRE_APPROVAL,
}
