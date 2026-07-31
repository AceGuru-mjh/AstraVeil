package com.astraveil.modules

import com.astraveil.core.AstraCore
import com.astraveil.core.event.ModuleInstalledEvent
import com.astraveil.core.event.ModuleStateChangedEvent
import com.astraveil.core.event.ModuleUninstalledEvent
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/**
 * Owns the full life-cycle of `.avm` packages on this device.
 *
 * **The .avm format** is a ZIP archive with the following layout:
 *
 * ```
 * my-module.avm
 * ├── module.json          # serialised [ModuleManifest], required
 * ├── runtime/
 * │   └── arm64.so         # native runtime, dlopen'd by [ModuleRuntime]
 * ├── assets/              # arbitrary files shipped with the module
 * └── permission.json      # optional, machine-readable permission rationale
 * ```
 *
 * `module.json` is validated up-front by [ModuleValidator]; the package is
 * only unpacked into `context.filesDir/astra_modules/<id>/` once validation
 * passes. Permissions declared in the manifest are routed through
 * [AstraCore.permissionEngine] so the user can approve or deny each token.
 *
 * @param context          Android context used to resolve `filesDir`.
 * @param core             The AstraVeil core engine (event bus, permission
 *                        engine, ...).
 * @param providerRegistry Used by `execute`-style operations that need root
 *                        and for sandbox mount plumbing.
 */
class ModuleManager(
    private val context: android.content.Context,
    private val core: AstraCore,
    private val providerRegistry: ProviderRegistry,
    private val runtime: ModuleRuntime? = null,
) {

    private val validator = ModuleValidator()
    private val mutex = Mutex()

    /** All registered modules keyed by id. */
    private val modules = mutableMapOf<String, AstraModule>()

    // P0-3 fix: unpack safety constants
    companion object {
        private const val MAX_ENTRIES = 1024
        private const val MAX_SINGLE_FILE_BYTES = 50L * 1024 * 1024   // 50MB
        private const val MAX_TOTAL_BYTES = 200L * 1024 * 1024        // 200MB
        private const val MAX_COMPRESSION_RATIO = 100.0
        private val MODULE_ID_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}$")
    }

    /** Root directory under which every module is unpacked. */
    private val modulesRoot: File = File(context.filesDir, "astra_modules").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Install the `.avm` package at [avmFile].
     *
     * Steps:
     *  1. Validate the archive (ZIP magic, presence of `module.json`).
     *  2. Validate the manifest via [ModuleValidator.validateManifest].
     *  3. Unpack into `astra_modules/<id>/`.
     *  4. Validate the unpacked tree via [ModuleValidator.validatePackage].
     *  5. Request each declared permission through the permission engine.
     *  6. Register the resulting [AstraModule] and emit [ModuleInstalledEvent].
     *
     * Returns `Result.success(module)` on success, `Result.failure` with a
     * descriptive exception otherwise. The file system is left clean on
     * failure: any partial unpack is deleted.
     */
    suspend fun install(avmFile: File): Result<AstraModule> = withContext(Dispatchers.IO) {
        runCatching {
            require(avmFile.exists()) { "avm file does not exist: ${avmFile.absolutePath}" }
            require(avmFile.name.endsWith(".avm")) { "not an .avm file: ${avmFile.name}" }

            // 1. Open archive and locate module.json
            val zip = ZipFile(avmFile)
            val moduleJsonEntry = zip.getEntry("module.json")
                ?: error("module.json missing in ${avmFile.name}")
            val rawJson = zip.getInputStream(moduleJsonEntry).bufferedReader()
                .use { it.readText() }

            // 2. Validate manifest
            val manifest = validator.validateManifest(rawJson)
                .getOrElse { throw it }
            val moduleId = manifest.name

            // 2.5 P0-3 fix: validate module ID (prevent path injection)
            require(MODULE_ID_REGEX.matches(moduleId)) {
                "unsafe module id (must match ${MODULE_ID_REGEX.pattern}): '$moduleId'"
            }

            mutex.withLock {
                require(!modules.containsKey(moduleId)) {
                    "module '$moduleId' is already installed"
                }

                // 3. Unpack (P0-3 fix: Zip Slip + zip bomb protection)
                val target = File(modulesRoot, moduleId).apply { if (exists()) deleteRecursively() }
                target.mkdirs()
                val canonicalTarget = target.canonicalFile
                zip.use { z ->
                    val entries = z.entries().toList()
                    require(entries.size <= MAX_ENTRIES) {
                        "too many entries: ${entries.size} > $MAX_ENTRIES"
                    }
                    var totalBytes = 0L
                    for (entry in entries) {
                        val out = File(target, entry.name)
                        val canonicalOut = out.canonicalFile
                        // Zip Slip protection
                        require(canonicalOut.path.startsWith(canonicalTarget.path + File.separator) ||
                                canonicalOut == canonicalTarget) {
                            "Zip Slip detected: '${entry.name}' escapes module directory"
                        }
                        // Reject absolute paths and parent traversal
                        require(!entry.name.startsWith("/") && !entry.name.contains("..")) {
                            "unsafe entry path: '${entry.name}'"
                        }
                        if (entry.isDirectory) {
                            out.mkdirs()
                            continue
                        }
                        // Single file size limit
                        if (entry.size > MAX_SINGLE_FILE_BYTES) {
                            target.deleteRecursively()
                            throw IllegalStateException("single file too large: '${entry.name}' = ${entry.size} bytes")
                        }
                        out.parentFile?.mkdirs()
                        z.getInputStream(entry).use { input ->
                            out.outputStream().use { os ->
                                val buf = ByteArray(8192)
                                var n = input.read(buf)
                                while (n > 0) {
                                    totalBytes += n
                                    if (totalBytes > MAX_TOTAL_BYTES) {
                                        target.deleteRecursively()
                                        throw IllegalStateException("total extraction exceeds $MAX_TOTAL_BYTES bytes (zip bomb?)")
                                    }
                                    os.write(buf, 0, n)
                                    n = input.read(buf)
                                }
                            }
                        }
                        // Compression ratio check (zip bomb)
                        if (entry.compressedSize > 0) {
                            val ratio = entry.size.toDouble() / entry.compressedSize
                            if (ratio > MAX_COMPRESSION_RATIO) {
                                target.deleteRecursively()
                                throw IllegalStateException("suspicious compression ratio ${"%.1f".format(ratio)} on '${entry.name}' (zip bomb?)")
                            }
                        }
                    }
                }

                // 4. Validate the unpacked tree
                validator.validatePackage(target).getOrElse { e ->
                    target.deleteRecursively()
                    throw e
                }

                // 5. Request permissions (policy-gated + persisted)
                val granted = mutableSetOf<String>()
                for (perm in manifest.permissions) {
                    if (core.requestAndPersistPermission(moduleId, perm)) granted += perm
                }

                // 6. Register
                val module = AstraModule(
                    id = moduleId,
                    manifest = manifest,
                    state = ModuleState.INSTALLED,
                    installPath = target.absolutePath,
                    grantedPermissions = granted
                )
                modules[moduleId] = module
                core.eventBus.emit(
                    ModuleInstalledEvent(moduleId, manifest.version)
                )
                module
            }
        }
    }

    /** Remove the module from disk and from the registry. Returns `true` if the
     *  module was installed and has now been removed. Also revokes and
     *  persists the removal of all permissions the module held. */
    suspend fun uninstall(moduleId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val module = modules.remove(moduleId) ?: return@withLock false
            File(module.installPath).deleteRecursively()
            // Revoke all permissions and persist the change so they don't
            // silently re-appear on restart.
            core.permissionEngine.revokeAll(moduleId)
            core.config.update { cfg ->
                cfg.authorizedPackages = core.permissionEngine.dumpPermissions()
            }
            core.eventBus.emit(ModuleUninstalledEvent(moduleId))
            true
        }
    }

    /** Move an installed module into the [ModuleState.ENABLED] state. */
    suspend fun enable(moduleId: String) = transition(moduleId, ModuleState.ENABLED)

    /** Move a module back to [ModuleState.DISABLED]. */
    suspend fun disable(moduleId: String) = transition(moduleId, ModuleState.DISABLED)

    /**
     * Load the module's native runtime and transition to RUNNING.
     *
     * If a [ModuleRuntime] was supplied at construction, the module's .so
     * is loaded (System.load → JNI_OnLoad → best-effort entry symbol
     * invocation via NativeBridge.nativeInvokeModuleEntry) BEFORE the
     * state transition. If loading fails, the state is NOT changed and
     * the module remains in its previous state.
     *
     * If no runtime was supplied (Phase 0 backward compat), only the
     * state transition occurs.
     */
    suspend fun start(moduleId: String) {
        if (runtime != null) {
            val module = mutex.withLock {
                modules[moduleId] ?: error("module '$moduleId' not installed")
            }
            val loaded = runtime.load(module)
            if (!loaded) {
                android.util.Log.e(
                    "ModuleManager",
                    "Runtime load failed for '$moduleId'; state unchanged.",
                )
                return
            }
        }
        transition(moduleId, ModuleState.RUNNING)
    }

    /**
     * Unload the module's native runtime and transition to ENABLED.
     */
    suspend fun stop(moduleId: String) {
        if (runtime != null) {
            runtime.unload(moduleId)
        }
        transition(moduleId, ModuleState.ENABLED)
    }

    /** Snapshot of every registered module. */
    fun list(): List<AstraModule> = modules.values.toList()

    /** Look up a module by id. */
    fun get(moduleId: String): AstraModule? = modules[moduleId]

    // ---- internals --------------------------------------------------------

    /** Apply a state transition under the mutex and emit a change event. */
    private suspend fun transition(moduleId: String, target: ModuleState) {
        mutex.withLock {
            val current = modules[moduleId] ?: error("module '$moduleId' not installed")
            if (current.state == target) return@withLock
            modules[moduleId] = current.copy(state = target)
            core.eventBus.emit(
                ModuleStateChangedEvent(moduleId, target.name)
            )
        }
    }
}
