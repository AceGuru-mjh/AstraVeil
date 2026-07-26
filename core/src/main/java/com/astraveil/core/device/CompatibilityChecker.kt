package com.astraveil.core.device

/**
 * Checks an [AndroidProfile] against AstraVeil's supported range
 * (Android 10 / SDK 29 through Android 16 / SDK 36).
 *
 * Returns a [CompatibilityResult] with warnings for any condition that
 * is not fatal but deserves user attention (e.g. SELinux in permissive
 * mode, unknown kernel).
 */
class CompatibilityChecker {

    fun check(profile: AndroidProfile): CompatibilityResult {
        val warnings = mutableListOf<String>()

        if (profile.sdk < 29) {
            warnings.add("Android version too old (SDK ${profile.sdk} < 29)")
        }
        if (profile.sdk > 36) {
            warnings.add("Android version newer than tested (SDK ${profile.sdk})")
        }
        if (profile.selinux != "Enforcing" && profile.selinux != "unknown") {
            warnings.add("SELinux is not enforcing (got ${profile.selinux})")
        }
        if (profile.kernel == "unknown") {
            warnings.add("Kernel version could not be read")
        }

        return CompatibilityResult(
            supported = warnings.none { it.contains("too old") },
            warnings = warnings,
        )
    }
}
