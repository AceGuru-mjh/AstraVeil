package com.astraveil.modules.runtime.sandbox

/**
 * Derives a [SandboxProfile] from a module's risk score.
 *
 *   risk < 30  → locked down (no fs, no ns, no property)
 *   risk < 70  → namespace + filesystem, no property
 *   risk >= 70 → full isolation profile (all granted, but requires approval)
 *
 * The resolver is the single place where risk → sandbox mapping lives,
 * so changing the policy is a one-file edit.
 */
class SandboxPolicyResolver {

    fun resolve(risk: Int): SandboxProfile = when {
        risk < 30 -> SandboxProfile(
            filesystem = false,
            namespace = false,
            property = false,
            maxRisk = 30,
        )
        risk < 70 -> SandboxProfile(
            filesystem = true,
            namespace = true,
            property = false,
            maxRisk = 70,
        )
        else -> SandboxProfile(
            filesystem = true,
            namespace = true,
            property = true,
            maxRisk = 100,
        )
    }
}
