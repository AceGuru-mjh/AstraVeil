package com.astraveil.sdk

/**
 * Context handed to a loaded AVM module.
 *
 * Modules never call `su` directly — they go through this interface so
 * every root operation is attributed, permission-checked, and audited.
 *
 * Usage inside a module's `avm_on_load`:
 * @code
 * if (ctx.requestCapability("root_execution")) {
 *     val result = ctx.execute("id")
 *     // result == "uid=0(root) ..."
 * }
 * @endcode
 */
interface AstraModuleContext {

    /** Request [capability] for this module. Returns true if granted. */
    suspend fun requestCapability(capability: String): Boolean

    /** Execute @p command through the active root provider. */
    suspend fun execute(command: String): String
}
