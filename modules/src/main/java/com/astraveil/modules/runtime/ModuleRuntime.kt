package com.astraveil.modules.runtime

import com.astraveil.modules.api.ModuleManifest
import com.astraveil.modules.runtime.validator.ModuleValidator

/**
 * v3 module runtime — owns the installed-module registry and drives the
 * install → validate → start → stop lifecycle.
 *
 * Phase 3 skeleton: [install] validates + registers; [start]/[stop] are
 * stubs that become real when the daemon [ModuleRunner] is wired (the
 * runner spawns an isolated process per module).
 */
class ModuleRuntime {

    private val modules = mutableMapOf<String, ModuleRecord>()
    private val validator = ModuleValidator()

    /** Validate + register @p manifest. Returns false on validation failure. */
    fun install(manifest: ModuleManifest): Boolean {
        if (!validator.validate(manifest)) return false
        modules[manifest.id] = ModuleRecord(
            id = manifest.id,
            state = ModuleState.INSTALLED,
            installedAt = System.currentTimeMillis(),
        )
        return true
    }

    /** Transition @p moduleId to STARTING then RUNNING. */
    fun start(moduleId: String): Boolean {
        val record = modules[moduleId] ?: return false
        modules[moduleId] = record.copy(state = ModuleState.RUNNING)
        return true
    }

    /** Transition @p moduleId to STOPPED. */
    fun stop(moduleId: String): Boolean {
        val record = modules[moduleId] ?: return false
        modules[moduleId] = record.copy(state = ModuleState.STOPPED)
        return true
    }

    /** Remove @p moduleId from the registry. */
    fun uninstall(moduleId: String): Boolean {
        return modules.remove(moduleId) != null
    }

    /** Snapshot of every registered module. */
    fun list(): List<ModuleRecord> = modules.values.toList()
}
