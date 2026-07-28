package com.astraveil.app.repository

import android.content.Context
import android.net.Uri
import com.astraveil.app.AstraVeilApplication
import com.astraveil.app.di.AstraContainer
import com.astraveil.core.modules.manifest.AvmManifestParser
import com.astraveil.core.modules.model.ModuleInfo
import com.astraveil.core.modules.model.ModulePermissionInfo
import com.astraveil.core.modules.model.ModuleUiState
import com.astraveil.core.modules.model.RiskSource
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
     * Reads only the `module.json` entry from the ZIP archive.
     * Does not copy the file, does not touch the filesystem,
     * does not invoke [ModuleManager].
     *
     * @return parsed [ModuleInfo] for display in the install dialog,
     *         or null if the archive is invalid.
     */
    suspend fun preview(uri: Uri): AvmManifestParser.PreviewResult

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

    override suspend fun preview(uri: Uri): AvmManifestParser.PreviewResult =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext AvmManifestParser.PreviewResult.Failure(
                        AvmManifestParser.PreviewError.IO_ERROR
                    )
                inputStream.use { stream ->
                    AvmManifestParser.parse(stream)
                }
            } catch (e: Exception) {
                AvmManifestParser.PreviewResult.Failure(
                    AvmManifestParser.PreviewError.IO_ERROR
                )
            }
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
                risk = estimateRisk(perm),
                reason = estimateReason(perm),
                riskSource = RiskSource.ESTIMATED, // Phase-0 manifest: no real risk data
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

// Renamed from defaultRisk → estimateRisk to make provenance explicit
private fun estimateRisk(capability: String): Int = when (capability) {
    "su", "root_execution" -> 90
    "kernel_hook" -> 95
    "boot_patch" -> 85
    "selinux_control" -> 80
    "system_write" -> 75
    "mount", "mount_namespace" -> 70
    "overlayfs" -> 65
    "namespace" -> 60
    "network" -> 40
    "filesystem" -> 30
    "property" -> 20
    else -> 10
}

private fun estimateReason(capability: String): String = when (capability) {
    "su", "root_execution" -> "Execute commands as root"
    "kernel_hook" -> "Hook kernel functions"
    "boot_patch" -> "Modify boot image"
    "selinux_control" -> "Modify SELinux policy"
    "system_write" -> "Write to /system partition"
    "mount", "mount_namespace" -> "Mount filesystem overlays"
    "overlayfs" -> "OverlayFS mount operations"
    "namespace" -> "Create isolated namespaces"
    "network" -> "Open network sockets"
    "filesystem" -> "Read/write filesystem paths"
    "property" -> "Read/write system properties"
    else -> "Capability: $capability"
}

object ModuleRepositoryProvider {
    private var instance: ModuleRepository? = null

    fun get(context: Context): ModuleRepository {
        return instance ?: ModuleRepositoryImpl(context.applicationContext).also {
            instance = it
        }
    }
}
