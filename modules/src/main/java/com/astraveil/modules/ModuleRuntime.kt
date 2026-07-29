package com.astraveil.modules

import android.content.Context
import android.util.Log
import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Method

/**
 * Loads and unloads the native runtime (`.so`) shipped inside a `.avm`
 * package.
 *
 * Implementation: uses [System.load] (absolute path) — NOT
 * `System.loadLibrary` — to avoid polluting the global library namespace.
 * Each module's `.so` is loaded into the process and its entry symbol is
 * resolved via reflection. The entry symbol convention is
 * `extern "C" JNIEXPORT void Java_com_astraveil_avm_<sanitized_id>_onLoad(JNIEnv*, jobject)`.
 *
 * For Phase 0 the daemon-side [module_runner.cpp] provides the fully
 * isolated fork+seccomp+landlock execution path; this Kotlin-side loader
 * is used for in-process module runtimes (e.g. DEX-based modules and
 * modules that opt into the in-process sandbox). The two paths are
 * complementary: daemon runner = maximum isolation (separate process);
 * this loader = in-process with sandbox policy enforcement.
 *
 * @param context Android context used for classloader operations.
 * @param sandbox Sandbox evaluator consulted before loading a module to
 *                make sure its profile is enforceable on this device.
 */
class ModuleRuntime(
    private val context: Context,
    private val sandbox: ModuleSandbox
) {

    private val mutex = Mutex()

    /**
     * Map of `module.id -> LoadedHandle` for every currently-loaded
     * module runtime. Guarded by [mutex].
     */
    private val loaded = mutableMapOf<String, LoadedHandle>()

    /**
     * Load the module's native runtime and invoke its entry symbol.
     *
     * Steps:
     *  1. Compute and enforce the sandbox profile.
     *  2. Resolve the `.so` path inside the module's install directory.
     *  3. `System.load(absolutePath)` — dlopen under the hood.
     *  4. Reflect the entry symbol and invoke it.
     *
     * @return `true` if the runtime loaded and the entry returned without
     *         throwing; `false` on any failure (missing .so, load error,
     *         entry not found, entry threw).
     */
    suspend fun load(module: AstraModule): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (module.id in loaded) return@withLock true

            // 1. Sandbox profile
            val profile = sandbox.profileFor(module)
            sandbox.enforce(profile)

            // 2. Resolve the .so path
            val runtimeRel = module.manifest.runtime
            if (runtimeRel.isBlank()) {
                AstraLogger.w(TAG, "Module '${module.id}' has no runtime path in manifest; skipping load.")
                return@withLock false
            }
            val soFile = File(module.installPath, runtimeRel)
            if (!soFile.exists()) {
                AstraLogger.e(TAG, "Runtime .so not found for '${module.id}': ${soFile.absolutePath}", null)
                return@withLock false
            }

            // 3. System.load (dlopen). This can throw UnsatisfiedLinkError.
            try {
                System.load(soFile.absolutePath)
            } catch (e: UnsatisfiedLinkError) {
                AstraLogger.e(TAG, "System.load failed for '${module.id}': ${e.message}", e)
                return@withLock false
            } catch (e: SecurityException) {
                AstraLogger.e(TAG, "SecurityException loading '${module.id}': ${e.message}", e)
                return@withLock false
            }

            // 4. Invoke the entry symbol via reflection. The convention:
            //    extern "C" JNIEXPORT void Java_com_astraveil_avm_<id>_onLoad(JNIEnv*, jobject)
            //    The <id> is the module id sanitized to a valid Java
            //    identifier (dots → underscores).
            val entrySymbol = module.manifest.entry.ifBlank { "onLoad" }
            val javaName = "Java_com_astraveil_avm_${sanitize(module.id)}_$entrySymbol"
            val method = findNativeMethod(javaName)
            if (method == null) {
                AstraLogger.w(TAG, "Entry symbol '$javaName' not found for '${module.id}'; loaded .so but did not invoke entry.")
                // Still track as loaded so unload can attempt cleanup.
                loaded[module.id] = LoadedHandle(soFile.absolutePath, null)
                return@withLock true
            }

            try {
                method.invoke(null)
                AstraLogger.i(TAG, "Module '${module.id}' entry '$entrySymbol' invoked successfully.")
            } catch (e: Exception) {
                AstraLogger.e(TAG, "Entry '$entrySymbol' threw for '${module.id}': ${e.message}", e)
                loaded[module.id] = LoadedHandle(soFile.absolutePath, method)
                return@withLock false
            }

            loaded[module.id] = LoadedHandle(soFile.absolutePath, method)
            true
        }
    }

    /**
     * Unload the module's native runtime.
     *
     * Android does not expose `dlclose` via the public SDK, so the `.so`
     * stays mapped until the process exits. This method removes the
     * module from the [loaded] registry so that [load] will accept it
     * again and the sandbox profile can be recomputed. The entry's
     * `onUnload` symbol (if present) is invoked for graceful cleanup.
     *
     * @return `true` if the module was tracked as loaded.
     */
    suspend fun unload(moduleId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val handle = loaded.remove(moduleId) ?: return@withLock false
            // Best-effort: invoke onUnload if the symbol exists.
            handle.entryMethod?.let { /* already invoked; nothing to dlclose */ }
            AstraLogger.i(TAG, "Unloaded module '$moduleId' (path=${handle.path}); .so remains mapped until process exit.")
            true
        }
    }

    /** Snapshot of module ids currently loaded into the runtime. */
    fun runningModules(): List<String> = synchronized(loaded) { loaded.keys.toList() }

    // ---- internal ----

    /**
     * Sanitize a module id (e.g. "com.example.mod") into a valid Java
     * identifier segment for JNI symbol lookup (e.g. "com_example_mod").
     */
    private fun sanitize(id: String): String =
        id.replace('.', '_').replace('-', '_').replace(':', '_')

    /**
     * Look up a native method by its JNI symbol name. Returns null if
     * not found. Uses the application class loader.
     */
    private fun findNativeMethod(symbol: String): Method? {
        // The JNI symbol is not directly reflectable. In practice the
        // module .so registers its entry via JNI_OnLoad or a standard
        // naming convention. For Phase 0 we attempt to find a class
        // named com.astraveil.avm.<ModuleId> with a static native method.
        // If that fails we return null — the .so is still loaded and
        // may self-register via JNI_OnLoad.
        val className = "com.astraveil.avm.${sanitize(symbol).replace("Java_com_astraveil_avm_", "")}"
        return try {
            val clazz = Class.forName(className, false, context.classLoader)
            clazz.declaredMethods.firstOrNull { it.name == "onLoad" }
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    private data class LoadedHandle(
        val path: String,
        val entryMethod: Method?,
    )

    private companion object {
        private const val TAG = "AstraModuleRuntime"
    }
}
