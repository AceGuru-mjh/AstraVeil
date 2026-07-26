package com.astraveil.core.version

/**
 * AstraVeil version constants.
 *
 * Read by the About screen, the startup diagnostics, and the update
 * manager. Bumped at release time.
 */
object Version {
    const val NAME = "AstraVeil"
    const val VERSION = "0.1.0-alpha"
    const val API = 2
    const val DEVELOPER = "MJH"

    /** Full display string for the About screen. */
    fun displayString(): String = "$NAME v$VERSION"
}
