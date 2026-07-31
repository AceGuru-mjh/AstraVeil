package com.astraveil.modules.security

/**
 * Interim policy (P0-4) governing whether a module's native code may be
 * loaded INTO the app process via `System.load()`.
 *
 * Until the isolated ModuleRunner (daemon fork + dlopen) lands, loading
 * arbitrary third-party .so into the UI process is unsafe. So:
 *   - no native lib              -> ALLOW (pure Kotlin/config module)
 *   - built-in module            -> ALLOW (shipped & vetted with the app)
 *   - OFFICIAL signature         -> ALLOW (AstraVeil team signs it)
 *   - anything else w/ native    -> REQUIRE_ISOLATION (refuse for now)
 *
 * This is honest: we do NOT pretend to sandbox; we refuse to load
 * untrusted native code in-process until real isolation exists.
 *
 * Phase 1 will replace the `REQUIRE_ISOLATION` refusal with an IPC
 * hand-off to the daemon's isolated ModuleRunner (fork + seccomp +
 * landlock + namespace + dlopen). Until then, third-party native
 * modules simply cannot run — which is the safe default.
 */
object NativeModuleLoadPolicy {

    enum class Decision {
        ALLOW,                // safe to load in-process
        REQUIRE_ISOLATION,    // needs ModuleRunner (Phase 1); refuse for now
    }

    /**
     * Module ids packaged with the app and vetted by the team.
     *
     * Built-in modules are shipped inside the APK and their .so files
     * are reviewed with the same scrutiny as the app itself, so they
     * are exempt from the third-party isolation requirement.
     */
    private val BUILT_IN_MODULES = setOf(
        "com.astraveil.builtin.diags",
        // add other first-party built-in module ids here
    )

    fun decide(
        moduleId: String,
        hasNativeLib: Boolean,
        trustLevel: TrustLevel,
    ): Decision {
        // No native code -> nothing to isolate.
        if (!hasNativeLib) return Decision.ALLOW

        // Built-in (first-party, shipped with app) -> trusted.
        if (moduleId in BUILT_IN_MODULES) return Decision.ALLOW

        // Official release signature -> team vouches for the native code.
        if (trustLevel == TrustLevel.OFFICIAL) return Decision.ALLOW

        // Third-party / developer-signed / unsigned native module:
        // must run in the isolated ModuleRunner, which is Phase 1.
        return Decision.REQUIRE_ISOLATION
    }

    /** User-facing reason when a module is refused. */
    fun refusalReason(moduleId: String, trustLevel: TrustLevel): String =
        "Module '$moduleId' contains native code signed as $trustLevel. " +
            "Third-party native modules must run in the isolated ModuleRunner " +
            "(Phase 1) and cannot be loaded in-process in this Alpha. " +
            "Only built-in or officially-signed native modules are supported."
}
