package com.astraveil.app.ui.screens.modules

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.repository.InspectionResult
import com.astraveil.app.repository.ModulePreview
import com.astraveil.app.repository.ModuleRepositoryProvider
import com.astraveil.core.modules.model.ModuleInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Unified operation feedback state for any module action
 * (install / start / stop / uninstall). Patch 18.2.3.
 *
 * Replaces the old `installing: Boolean` + scattered `error`/`success`
 * strings with a single sealed type the UI can render exhaustively.
 */
sealed class ModuleOperationState {
    /** No operation in flight. */
    data object Idle : ModuleOperationState()

    /** Operation running — UI shows a spinner / "Installing…" label. */
    data object Loading : ModuleOperationState()

    /** Operation succeeded — UI shows a transient success message. */
    data class Success(val message: String) : ModuleOperationState()

    /** Operation failed — UI shows the error message. */
    data class Error(val message: String) : ModuleOperationState()
}

/**
 * UI state for the Module Center screen.
 *
 * @property installState  State of the install flow (preview → confirm).
 * @property moduleOperations Per-module state for start / stop / uninstall,
 *                            keyed by module id.
 */
data class ModulesUiState(
    val modules: List<ModuleInfo> = emptyList(),
    val loading: Boolean = false,
    val installState: ModuleOperationState = ModuleOperationState.Idle,
    val moduleOperations: Map<String, ModuleOperationState> = emptyMap(),
)

/**
 * State machine for the install-preview flow.
 *
 * ```
 * IDLE → PREVIEWING → READY (dialog shown)
 *                    → FAILED (error snackbar)
 * READY → (confirmInstall) → installState.Loading → IDLE (success/error)
 * ```
 */
sealed class PreviewState {
    /** No file selected, no dialog. */
    data object Idle : PreviewState()

    /** Reading + parsing the .avm manifest. */
    data object Previewing : PreviewState()

    /** Manifest parsed successfully. Dialog should show [preview]. */
    data class Ready(
        val preview: ModulePreview,
        val uri: Uri,
    ) : PreviewState()

    /** Manifest parse failed. Show [reason] to the user. */
    data class Failed(val reason: String) : PreviewState()
}

/**
 * ViewModel for the AVM Module Center.
 *
 * Data flow:
 * ```
 * ModulesScreen → ModulesViewModel → ModuleRepository → ModuleManager
 *                                       └─ ModuleInspector → AvmManifestParser
 * ```
 */
class ModulesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ModuleRepositoryProvider.get(app)

    private val _uiState = MutableStateFlow(ModulesUiState())
    val uiState: StateFlow<ModulesUiState> = _uiState.asStateFlow()

    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Idle)
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()

    init {
        refresh()
    }

    /** Reload the module list from the repository. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val modules = repository.listModules()
                _uiState.update { it.copy(loading = false, modules = modules) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        installState = ModuleOperationState.Error(
                            "Failed to load modules: ${t.message ?: "unknown error"}"
                        ),
                    )
                }
            }
        }
    }

    /**
     * Step 1 of install flow: pre-parse the .avm manifest.
     *
     * Called when the file picker returns a URI.
     * Does NOT install anything. Only reads module.json from the ZIP.
     */
    fun previewUri(uri: Uri) {
        viewModelScope.launch {
            _previewState.value = PreviewState.Previewing
            when (val result = repository.preview(uri)) {
                is InspectionResult.Success -> {
                    _previewState.value = PreviewState.Ready(
                        preview = result.preview,
                        uri = uri,
                    )
                }
                is InspectionResult.Failure -> {
                    _previewState.value = PreviewState.Failed(
                        reason = result.reason,
                    )
                }
            }
        }
    }

    /**
     * Step 2 of install flow: user confirmed in the dialog.
     * Actually installs the .avm via ModuleManager.
     */
    fun confirmInstall() {
        val state = _previewState.value
        if (state !is PreviewState.Ready) return

        viewModelScope.launch {
            _uiState.update { it.copy(installState = ModuleOperationState.Loading) }
            try {
                val installed = repository.install(state.uri)
                _uiState.update {
                    it.copy(
                        installState = ModuleOperationState.Success(
                            "'${installed.name}' installed."
                        ),
                        modules = it.modules + installed,
                    )
                }
                _previewState.value = PreviewState.Idle
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        installState = ModuleOperationState.Error(
                            "Install failed: ${t.message ?: "unknown error"}"
                        ),
                    )
                }
                _previewState.value = PreviewState.Idle
            }
        }
    }

    /** Dismiss the install dialog without installing. */
    fun cancelPreview() {
        _previewState.value = PreviewState.Idle
    }

    /** Uninstall a module by id. */
    fun uninstall(moduleId: String) {
        viewModelScope.launch {
            setModuleOp(moduleId, ModuleOperationState.Loading)
            try {
                repository.uninstall(moduleId)
                _uiState.update {
                    it.copy(
                        modules = it.modules.filter { m -> m.id != moduleId },
                        moduleOperations = it.moduleOperations - moduleId,
                    )
                }
                _uiState.update {
                    it.copy(installState = ModuleOperationState.Success("Module uninstalled."))
                }
            } catch (t: Throwable) {
                setModuleOp(
                    moduleId,
                    ModuleOperationState.Error("Uninstall failed: ${t.message ?: "unknown error"}"),
                )
            }
        }
    }

    /** Start a module. */
    fun start(moduleId: String) {
        viewModelScope.launch {
            setModuleOp(moduleId, ModuleOperationState.Loading)
            try {
                repository.start(moduleId)
                setModuleOp(moduleId, ModuleOperationState.Success("Started."))
                refresh()
            } catch (t: Throwable) {
                setModuleOp(
                    moduleId,
                    ModuleOperationState.Error("Start failed: ${t.message ?: "unknown error"}"),
                )
            }
        }
    }

    /** Stop a module. */
    fun stop(moduleId: String) {
        viewModelScope.launch {
            setModuleOp(moduleId, ModuleOperationState.Loading)
            try {
                repository.stop(moduleId)
                setModuleOp(moduleId, ModuleOperationState.Success("Stopped."))
                refresh()
            } catch (t: Throwable) {
                setModuleOp(
                    moduleId,
                    ModuleOperationState.Error("Stop failed: ${t.message ?: "unknown error"}"),
                )
            }
        }
    }

    /** Clear the install operation state (after the UI has shown it). */
    fun clearInstallState() {
        _uiState.update {
            it.copy(installState = ModuleOperationState.Idle)
        }
    }

    /** Clear a per-module operation state. */
    fun clearModuleOp(moduleId: String) {
        _uiState.update {
            it.copy(moduleOperations = it.moduleOperations - moduleId)
        }
    }

    private fun setModuleOp(moduleId: String, state: ModuleOperationState) {
        _uiState.update {
            it.copy(moduleOperations = it.moduleOperations + (moduleId to state))
        }
    }
}
