package com.astraveil.app.repository

import android.content.Context
import android.net.Uri
import com.astraveil.app.AstraVeilApplication
import com.astraveil.app.di.AstraContainer
import com.astraveil.core.modules.model.ModuleInfo
import com.astraveil.core.modules.model.ModulePermissionInfo
import com.astraveil.core.modules.model.ModuleUiState
import com.astraveil.modules.AstraModule
import com.astraveil.modules.ModuleManager
import com.astraveil.modules.ModuleRuntime
import com.astraveil.modules.ModuleSandbox
import com.astraveil.modules.ModuleState
import com.astraveil.app.notification.AstraNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface ModuleRepository {

    /** List every installed module, adapted to [ModuleInfo]. */
    suspend fun listModules(): List<ModuleInfo>

    /**
     * Pre-scan a `.avm` package from a content [uri] WITHOUT installing.
     *
     * Delegates to [ModuleInspector]. Computes SHA-256, parses
     * `module.json`, and returns a [ScanResult] containing a
     * [com.astraveil.core.modules.security.TrustReport] on success.
     */
    suspend fun preview(uri: Uri): ScanResult

    /**
     * Install a `.avm` package from a content [uri].
     *
     * The file is copied to a single staging location first (P0-5 TOCTOU:
     * the URI is opened exactly ONCE, not twice as in the old preview→install
     * flow). If [expectedHash] is non-null it is re-verified against the
     * staged file immediately before installation — the bytes the user
     * reviewed in the preview dialog MUST be the bytes that get installed.
     *
     * @param expectedHash SHA-256 computed during [preview], or null to
     *                     skip the TOCTOU re-check (TrustGate still runs).
     * @return the installed [ModuleInfo], or throws on failure.
     */
    suspend fun install(uri: Uri, expectedHash: String? = null): ModuleInfo

    /** Uninstall by module id. Returns true if the module existed. */
    suspend fun uninstall(id: String): Boolean

    /** Start (transition to RUNNING). */
    suspend fun start(id: String)

    /** Stop (transition back to ENABLED / STOPPED). */
    suspend fun stop(id: String)
}

class ModuleRepositoryImpl(
    private val context: Context,
) : ModuleRepository {

    private val manager: ModuleManager by lazy {
        // Wire the in-process ModuleRuntime so start()/stop() actually
        // load/unload the module's .so (System.load + JNI_OnLoad +
        // NativeBridge.nativeInvokeModuleEntry). Without this, ModuleManager
        // only flips the state flag without executing any module code.
        val sandbox = ModuleSandbox(AstraVeilApplication.core)
        val runtime = ModuleRuntime(context, sandbox)
        ModuleManager(
            context = context,
            core = AstraVeilApplication.core,
            providerRegistry = AstraContainer.providerRegistry,
            runtime = runtime,
        )
    }

    override suspend fun listModules(): List<ModuleInfo> = withContext(Dispatchers.IO) {
        manager.list().map { it.toModuleInfo() }
    }

    override suspend fun preview(uri: Uri): ScanResult =
        withContext(Dispatchers.IO) {
            ModuleInspector.inspect(context, uri)
        }

    override suspend fun install(uri: Uri, expectedHash: String?): ModuleInfo = withContext(Dispatchers.IO) {
        // P0-5: single staging copy — the URI is opened exactly once.
        val staging = File(context.cacheDir, "staged_${System.currentTimeMillis()}.avm")
        context.contentResolver.openInputStream(uri)?.use { input ->
            staging.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open input stream for $uri")

        try {
            // TOCTOU closure: if the caller passed the hash computed during
            // preview, re-verify it on the staged file. The bytes the user
            // reviewed MUST equal the bytes being installed.
            if (expectedHash != null) {
                val actual = com.astraveil.app.update.UpdateVerifier.computeSha256(staging)
                if (actual == null || !actual.equals(expectedHash, ignoreCase = true)) {
                    throw SecurityException(
                        "Staged file changed since review (hash mismatch: " +
                            "expected=$expectedHash, actual=$actual)"
                    )
                }
            }
            // TrustGate runs again inside ModuleManager.install as defense-in-depth.
            val result = manager.install(staging)
            val module = result.getOrElse { throw it }
            val info = module.toModuleInfo()
            AstraNotificationManager.notifyModuleChange(
                context = context,
                moduleName = info.name,
                installed = true,
            )
            info
        } finally {
            staging.delete()
        }
    }

    override suspend fun uninstall(id: String): Boolean = withContext(Dispatchers.IO) {
        val success = manager.uninstall(id)
        if (success) {
            AstraNotificationManager.notifyModuleChange(
                context = context,
                moduleName = id,
                installed = false,
            )
        }
        success
    }

    override suspend fun start(id: String) = withContext(Dispatchers.IO) {
        manager.start(id)
    }

    override suspend fun stop(id: String) = withContext(Dispatchers.IO) {
        manager.stop(id)
    }
}

// ---- Adapter: AstraModule → ModuleInfo ----
//
// The :app adapter is the ONLY place that produces the UI-facing
// ModuleInfo. Patch 18.2.1: risk is honestly `null` for Phase-0
// installed modules (no heuristic). When ModuleManager gains v3
// manifest support, this adapter will forward the declared riskLevel.

internal fun AstraModule.toModuleInfo(): ModuleInfo {
    return ModuleInfo(
        id = id,
        name = manifest.name,
        version = manifest.version,
        description = manifest.description,
        state = state.toUiState(),
        permissions = manifest.permissions.map { perm ->
            ModulePermissionInfo(
                capability = perm,
                risk = null,        // Phase-0 manifest: undeclared → Unknown
                reason = "",
            )
        },
    )
}

private fun ModuleState.toUiState(): ModuleUiState = when (this) {
    ModuleState.RUNNING -> ModuleUiState.RUNNING
    ModuleState.ERROR -> ModuleUiState.FAILED
    ModuleState.INSTALLED -> ModuleUiState.INSTALLED
    ModuleState.ENABLED -> ModuleUiState.STOPPED
    ModuleState.DISABLED -> ModuleUiState.STOPPED
}

object ModuleRepositoryProvider {
    private var instance: ModuleRepository? = null

    fun get(context: Context): ModuleRepository {
        return instance ?: ModuleRepositoryImpl(context.applicationContext).also {
            instance = it
        }
    }
}
