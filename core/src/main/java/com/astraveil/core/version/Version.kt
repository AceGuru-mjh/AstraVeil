package com.astraveil.core.version

/**
 * AstraVeil version constants.
 *
 * The version *string* and *code* are derived from
 * [com.astraveil.core.BuildConfig], which is itself generated from
 * `gradle.properties` (`astraveil.version` / `astraveil.versionCode`).
 *
 * NEVER hardcode a version string here. The previous hardcoded
 * `"0.1.0-alpha"` drifted out of sync with `gradle.properties` and the
 * git tags (1.2.1 / 1.0.0 / 0.1.0-alpha all coexisted); deriving from
 * a single source of truth closes that gap permanently.
 *
 * The product [NAME] ("AstraVeil") and [DEVELOPER] ("MJH") are brand
 * constants, not version metadata, so they remain hardcoded.
 *
 * Read by the About screen, the startup diagnostics, and the update
 * manager.
 */
object Version {
    const val NAME = "AstraVeil"

    /** Version string, e.g. `"1.5.0"`. Derived from gradle.properties. */
    val VERSION: String get() = com.astraveil.core.BuildConfig.ASTRAVEIL_VERSION

    /** Numeric version code, e.g. `15`. Derived from gradle.properties. */
    val CODE: Int get() = com.astraveil.core.BuildConfig.ASTRAVEIL_VERSION_CODE

    const val API = 2
    const val DEVELOPER = "MJH"

    /** Full display string for the About screen. */
    fun displayString(): String = "$NAME v$VERSION"
}
