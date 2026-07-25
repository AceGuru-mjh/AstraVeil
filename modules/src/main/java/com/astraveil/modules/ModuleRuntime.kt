package com.astraveil.modules

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads and unloads the native runtime (`.so`) shipped inside a `.avm` package.
 *
 * In Phase 0 this class is a deliberate stub: the `dlopen` / `dlsym` plumbing
 * is intentionally deferred to a later phase so that the rest of the module
 * life-cycle (`install`/`enable`/`disable`/...) can be exercised end-to-end
 * without a working native loader. Every member logs a clear "not yet
 * implemented" message and returns a benign value so callers can drive the
 * state machine without crashes.
 *
 * Future implementers:
 *  * Use `System.load(...)` (absolute path) — NOT `System.loadLibrary` — to
 *    avoid polluting the global library namespace.
 *  * The entry symbol is named in [ModuleManifest.entry]; resolve it via JNI
 *    and invoke it with a handle that exposes the sandbox profile and the
 *    [com.astraveil.sdk.AstraClient] facade back to the module.
 *  * Track every loaded `module_id -> handle` in [loaded] so [unload] can
 *    cleanly tear down via the matching `dlclose`-equivalent.
 *
 * @param context Android context (used in future phases to look up the
 *                application class loader).
 * @param sandbox Sandbox evaluator consulted before loading a module to make
 *                sure its profile is enforceable on this device.
 */
class ModuleRuntime(
    private val context: Context,
    private val sandbox: ModuleSandbox
) {

    private val mutex = Mutex()

    /** Map of `module.id -> loaded native handle` (placeholder until Phase 1). */
    private val loaded = mutableMapOf<String, Long>()

    /**
     * Load the module's native runtime and invoke its entry symbol.
     *
     * Phase 0 behaviour: returns `false` and logs. The sandbox profile is
     * still computed and "enforced" (currently a no-op) so that downstream
     * code paths can be exercised.
     */
    suspend fun load(module: AstraModule): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (module.id in loaded) return@withLock true
            val profile = sandbox.profileFor(module)
            sandbox.enforce(profile)
            Log.w(
                TAG,
                "Module runtime not yet implemented in Phase 0; " +
                    "module '${module.id}' will not actually be loaded."
            )
            // TODO(Phase 1): val handle = nativeLoad(File(module.installPath, module.manifest.runtime).absolutePath)
            //                if (handle == 0L) return@withLock false
            //                nativeInvokeEntry(handle, module.manifest.entry)
            //                loaded[module.id] = handle
            false
        }
    }

    /**
     * Unload the module's native runtime.
     *
     * Phase 0 behaviour: returns `true` if the module was tracked as loaded
     * (which under Phase 0 is never, since [load] always returns `false`).
     */
    suspend fun unload(moduleId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val handle = loaded.remove(moduleId) ?: return@withLock false
            // TODO(Phase 1): nativeUnload(handle)
            Log.w(TAG, "Module runtime not yet implemented in Phase 0; unload is a no-op for '$moduleId'.")
            @Suppress("UNUSED_VARIABLE") val unused = handle
            true
        }
    }

    /** Snapshot of module ids currently loaded into the runtime. */
    fun runningModules(): List<String> = synchronized(loaded) { loaded.keys.toList() }

    // ---- JNI surface (placeholders, implemented in :native during Phase 1) ---

    // private external fun nativeLoad(path: String): Long
    // private external fun nativeInvokeEntry(handle: Long, symbol: String): Int
    // private external fun nativeUnload(handle: Long)

    private companion object {
        private const val TAG = "AstraModuleRuntime"
    }
}
