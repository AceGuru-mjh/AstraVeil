package com.astraveil.modules.runtime

/**
 * A registered module's runtime record.
 *
 * @property id          the module manifest id
 * @property state       current [ModuleState]
 * @property installedAt epoch millis when the module was installed
 */
data class ModuleRecord(
    val id: String,
    val state: ModuleState,
    val installedAt: Long,
)
