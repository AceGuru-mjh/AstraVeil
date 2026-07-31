package com.astraveil.modules

import android.content.Context
import android.util.Log
import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Loads and unloads the native runtime (`.so`) shipped inside a `.avm`
 * package, in-process.
 *
 * ## Entry-symbol conventions
 *
 * A module `.so` may expose its entry via one of two mechanisms:
 *
 * 1. **`JNI_OnLoad` self-registration (preferred).** The .so's
 *    `JNI_OnLoad(JavaVM*, void*)` function runs automatically when
 *    [System.load] maps the library. The module registers its native
 *    methods via `RegisterNatives` and performs any self-initialisation
 *    it needs. Kotlin does NOT need to look up a symbol — [load] just
 *    returns `true` after `System.load` succeeds.
 *
 * 2. **C-exported `avm_on_load` / `avm_on_unload` symbols.** The .so
 *    exports `extern "C" void avm_on_load(void)` and optionally
 *    `extern "C" void avm_on_unload(void)`. These are invoked via the
 *    [com.astraveil.nativelib.NativeBridge] JNI surface (see
 *    `nativeLoadModuleEntry` / `nativeUnloadModuleEntry`).
 *
 * The manifest's `entry` field names the entry symbol; if blank, the
 * default `avm_on_load` is used. If the symbol is not found, [load]
 * still succeeds (the `JNI_OnLoad` path may have already initialised
 * the module).
 *
 * ## Complement to daemon-side runner
 *
 * This in-process loader is used for DEX-based modules and modules that
 * opt into the in-process sandbox. The daemon-side `module_runner.cpp`
 * (PR #39) provides fully isolated fork+seccomp+landlock execution for
 * native modules that need maximum isolation.
 *
 * @param context Android context used for classloader operations.
 * @param sandbox Sandbox evaluator consulted before loading a module.
 */
class ModuleRuntime(
    private val context: Context,
    private val sandbox: ModuleSandbox
) {

    private val mutex = Mutex()

    /** Map of `module.id -> LoadedHandle` for every loaded module. */
    private val loaded = mutableMapOf<String, LoadedHandle>()

    /**
     * Load the module's native runtime.
     *
     * Steps:
     *  1. Compute and enforce the sandbox profile.
     *  2. Resolve the `.so` path from the manifest's `runtime` field.
     *  3. `System.load(absolutePath)` — triggers `JNI_OnLoad` if present.
     *  4. Invoke the named entry symbol via NativeBridge (best-effort).
     *
     * @return `true` if the `.so` loaded successfully (JNI_OnLoad path is
     *         considered a success even if the named symbol is absent).
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

            // ── P0-4 interim mitigation ──────────────────────────────────
            // Until the isolated ModuleRunner (daemon fork + dlopen) lands,
            // refuse to load third-party / developer-signed / unsigned
            // native code INTO the app process. Only built-in or
            // officially-signed native modules may load in-process.
            // Everything else must wait for Phase 1 isolation.
            val trustLevel = runCatching {
                com.astraveil.modules.security.TrustLevel.valueOf(module.trustLevelName)
            }.getOrDefault(com.astraveil.modules.security.TrustLevel.UNSIGNED)
            val loadDecision = com.astraveil.modules.security.NativeModuleLoadPolicy.decide(
                moduleId = module.id,
                hasNativeLib = soFile.exists(),
                trustLevel = trustLevel,
            )
            if (loadDecision == com.astraveil.modules.security.NativeModuleLoadPolicy.Decision.REQUIRE_ISOLATION) {
                AstraLogger.e(
                    TAG,
                    com.astraveil.modules.security.NativeModuleLoadPolicy.refusalReason(module.id, trustLevel),
                    null,
                )
                return@withLock false
            }

            // 3. System.load — triggers JNI_OnLoad if the .so defines it.
            //    This is the primary entry path: modules self-register via
            //    RegisterNatives inside JNI_OnLoad.
            try {
                System.load(soFile.absolutePath)
            } catch (e: UnsatisfiedLinkError) {
                AstraLogger.e(TAG, "System.load failed for '${module.id}': ${e.message}", e)
                return@withLock false
            } catch (e: SecurityException) {
                AstraLogger.e(TAG, "SecurityException loading '${module.id}': ${e.message}", e)
                return@withLock false
            }

            // 4. Best-effort: invoke the named entry symbol via NativeBridge.
            //    If the symbol is absent, the module may have self-initialised
            //    via JNI_OnLoad — still count as loaded.
            val entrySymbol = module.manifest.entry.ifBlank { "avm_on_load" }
            val entryInvoked = tryInvokeEntry(soFile.absolutePath, entrySymbol)
            if (!entryInvoked) {
                AstraLogger.i(TAG, "Module '${module.id}' loaded via JNI_OnLoad; entry symbol '$entrySymbol' not invoked (absent or JNI_OnLoad self-registered).")
            } else {
                AstraLogger.i(TAG, "Module '${module.id}' entry '$entrySymbol' invoked successfully.")
            }

            loaded[module.id] = LoadedHandle(soFile.absolutePath, entrySymbol)
            true
        }
    }

    /**
     * Unload the module's native runtime.
     *
     * Android does not expose `dlclose` via the public SDK, so the `.so`
     * stays mapped until the process exits. This method invokes the
     * module's `avm_on_unload` symbol (if present) for graceful cleanup
     * and removes the module from the [loaded] registry.
     *
     * @return `true` if the module was tracked as loaded.
     */
    suspend fun unload(moduleId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val handle = loaded.remove(moduleId) ?: return@withLock false
            // Best-effort: invoke the unload symbol if present.
            tryInvokeEntry(handle.path, "avm_on_unload")
            AstraLogger.i(TAG, "Unloaded module '$moduleId' (path=${handle.path}); .so remains mapped until process exit.")
            true
        }
    }

    /** Snapshot of module ids currently loaded into the runtime. */
    fun runningModules(): List<String> = synchronized(loaded) { loaded.keys.toList() }

    // ---- internal ----

    /**
     * Attempt to invoke a C-exported symbol in the module's .so via the
     * NativeBridge JNI surface. Returns true if the symbol was found and
     * invoked without throwing; false if the symbol is absent or
     * NativeBridge is unavailable.
     *
     * This is best-effort: the primary entry path is JNI_OnLoad (step 3
     * of [load]). This call covers modules that export a plain C entry
     * symbol instead of using RegisterNatives.
     */
    private fun tryInvokeEntry(soPath: String, symbol: String): Boolean {
        val bridge = com.astraveil.nativelib.NativeBridge
        if (!bridge.isAvailable) return false
        return try {
            bridge.nativeInvokeModuleEntry(soPath, symbol)
        } catch (e: UnsatisfiedLinkError) {
            // nativeInvokeModuleEntry not linked in this build — the .so
            // is still loaded; JNI_OnLoad may have self-registered.
            false
        } catch (e: Exception) {
            AstraLogger.w(TAG, "Entry '$symbol' threw for '$soPath': ${e.message}")
            false
        }
    }

    /** Tracks a loaded module's .so path and entry symbol for cleanup. */
    private data class LoadedHandle(
        val path: String,
        val entrySymbol: String,
    )

    private companion object {
        private const val TAG = "AstraModuleRuntime"
    }
}
