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
import com.astraveil.modules.ModuleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface ModuleRepository {

    /** List every installed module, adapted to [ModuleInfo]. */
    suspend fun listModules(): List<ModuleInfo>

    /**
     * Pre-parse an `.avm` package from a content [uri] WITHOUT installing.
     *
     * Delegates to [ModuleInspector]. Reads only the `module.json` entry
     * from the ZIP archive; does not copy the file, does not touch the
     * filesystem, does not invoke [ModuleManager].
     */
    suspend fun preview(uri: Uri): InspectionResult

    /**
     * Install a `.avm` package from a content [uri].
     * The file is copied to a temp location first because
     * [ModuleManager.install] requires a [File].
     *
     * @return the installed [ModuleInfo], or throws on failure.
     */
    suspend fun install(uri: Uri): ModuleInfo

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
        ModuleManager(
            context = context,
            core = AstraVeilApplication.core,
            providerRegistry = AstraContainer.providerRegistry,
        )
    }

    override suspend fun listModules(): List<ModuleInfo> = withContext(Dispatchers.IO) {
        manager.list().map { it.toModuleInfo() }
    }

    override suspend fun preview(uri: Uri): InspectionResult =
        withContext(Dispatchers.IO) {
            ModuleInspector.inspect(context, uri)
        }

    override suspend fun install(uri: Uri): ModuleInfo = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "install_${System.currentTimeMillis()}.avm")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open input stream for $uri")

        try {
            val result = manager.install(tempFile)
            val module = result.getOrElse { throw it }
            module.toModuleInfo()
        } finally {
            tempFile.delete()
        }
    }

    override suspend fun uninstall(id: String): Boolean = withContext(Dispatchers.IO) {
        manager.uninstall(id)
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
// Patch 18.2.1: the adapter no longer estimates risk. Installed modules
// come from Phase-0 manifests (string-only permissions), so risk is
// honestly `null` — the UI renders "Unknown". When v3 manifest support
// lands in ModuleManager, this adapter will forward the declared
// riskLevel instead.

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
