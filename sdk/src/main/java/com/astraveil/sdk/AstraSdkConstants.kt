package com.astraveil.sdk

/**
 * Stable constants published by the AstraVeil SDK.
 *
 * Third-party .avm modules link against these values at compile time. Any
 * change here is a breaking change to the SDK contract and MUST be accompanied
 * by a [AstraClient.sdkApiLevel] bump.
 */
object AstraSdkConstants {

    /**
     * The Module API level implemented by this build of AstraVeil. Modules
     * declare their `api` (and `minApi`) inside `module.json`; AstraVeil
     * refuses to install any module whose declared `api` is greater than this
     * value (see [com.astraveil.modules.ModuleValidator]).
     */
    const val MODULE_API_LEVEL: Int = 1

    /**
     * Canonical permission tokens recognised by the AstraVeil permission
     * engine. Modules request a subset of these in their manifest; the user
     * approves them at install time. Any other token is rejected by
     * [com.astraveil.modules.ModuleValidator].
     */
    val SUPPORTED_PERMISSIONS: List<String> = listOf(
        "filesystem",
        "namespace",
        "property",
        "mount",
        "network",
        "shell"
    )

    /** File extension used for Astra module packages. */
    const val MODULE_EXTENSION: String = ".avm"
}
