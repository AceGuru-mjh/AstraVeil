package com.astraveil.modules.runtime.session

import kotlinx.serialization.Serializable

/**
 * A running module's session record.
 *
 * @property moduleId   the module manifest id
 * @property pid        the OS process id of the module runner process
 * @property startedAt  epoch millis when the session started
 * @property sandboxed  true iff the SandboxManager successfully confined the process
 */
@Serializable
data class ModuleSession(
    val moduleId: String,
    val pid: Int,
    val startedAt: Long,
    val sandboxed: Boolean,
)
