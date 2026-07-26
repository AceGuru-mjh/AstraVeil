package com.astraveil.sdk

/**
 * Module hot-update interface.
 *
 * AstraVeil's module ecosystem needs hot-replace: stop → snapshot →
 * swap files → re-validate → re-sandbox → restart, without a full
 * daemon restart.
 */
interface ModuleUpdater {

    /** Update @p moduleId to @p version. Returns true on success. */
    suspend fun update(moduleId: String, version: String): Boolean

    /** Rollback @p moduleId to the previous version. */
    suspend fun rollback(moduleId: String): Boolean
}
