package com.astraveil.modules.lifecycle

/**
 * v3 module lifecycle — the uniform surface every AVM module exposes.
 *
 * Replaces the ad-hoc install/enable/disable/start/stop calls on
 * [com.astraveil.modules.ModuleManager] with a single lifecycle the
 * runtime drives. Each transition is audited.
 */
interface ModuleLifecycle {
    /** Install the module package onto the device. */
    fun install()

    /** Start the module (launch the isolated runner process). */
    fun start()

    /** Stop the module. */
    fun stop()

    /** Uninstall the module and remove its data. */
    fun uninstall()
}
