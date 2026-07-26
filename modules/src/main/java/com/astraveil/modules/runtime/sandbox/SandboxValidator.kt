package com.astraveil.modules.runtime.sandbox

/**
 * Validates a [SandboxProfile] before it is applied.
 *
 * Rejects profiles with out-of-range risk or internally inconsistent
 * grants (e.g. property=true but filesystem=false is suspicious).
 */
class SandboxValidator {

    fun validate(profile: SandboxProfile): Boolean {
        if (profile.maxRisk > 100 || profile.maxRisk < 0) return false
        // Property writes imply filesystem access — reject the
        // inconsistent combination so a buggy resolver can't produce a
        // profile that grants property but denies filesystem.
        if (profile.property && !profile.filesystem) return false
        return true
    }
}
