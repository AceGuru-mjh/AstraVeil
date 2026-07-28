package com.astraveil.app.ui.screens.modules

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.repository.ModuleRepositoryProvider
import com.astraveil.core.modules.manifest.AvmManifestParser
import com.astraveil.core.modules.model.ModuleInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Module Center screen.
 */
data class ModulesUiState(
    val modules: List<ModuleInfo> = emptyList(),
    val loading: Boolean = false,
    val installing: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

/**
 * State machine for the install-preview flow.
 *
 * ```
 * IDLE → PREVIEWING → PREVIEW_READY (dialog shown)
 *                    → PREVIEW_FAILED (error snackbar)
 * PREVIEW_READY → INSTALLING → IDLE (success) / PREVIEW_FAILED (error)
 * ```
 */
sealed class PreviewState {
    /** No file selected, no dialog. */
    data object Idle : PreviewState()

    /** Reading + parsing the .avm manifest. */
    data object Previewing : PreviewState()

    /** Manifest parsed successfully. Dialog should show [module]. */
    data class Ready(
        val module: ModuleInfo,
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
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val modules = repository.listModules()
                _uiState.update { it.copy(loading = false, modules = modules) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(loading = false, error = t.message) }
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
                is AvmManifestParser.PreviewResult.Success -> {
                    _previewState.value = PreviewState.Ready(
                        module = result.module,
                        uri = uri,
                    )
                }
                is AvmManifestParser.PreviewResult.Failure -> {
                    _previewState.value = PreviewState.Failed(
                        reason = result.reason.message,
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
            _uiState.update { it.copy(installing = true, error = null, successMessage = null) }
            try {
                val installed = repository.install(state.uri)
                _uiState.update {
                    it.copy(
                        installing = false,
                        modules = it.modules + installed,
                        successMessage = "'${installed.name}' installed successfully.",
                    )
                }
                _previewState.value = PreviewState.Idle
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(installing = false, error = "Install failed: ${t.message}")
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
            try {
                repository.uninstall(moduleId)
                _uiState.update {
                    it.copy(
                        modules = it.modules.filter { m -> m.id != moduleId },
                        successMessage = "Module uninstalled.",
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = "Uninstall failed: ${t.message}") }
            }
        }
    }

    /** Start a module. */
    fun start(moduleId: String) {
        viewModelScope.launch {
            try {
                repository.start(moduleId)
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = "Start failed: ${t.message}") }
            }
        }
    }

    /** Stop a module. */
    fun stop(moduleId: String) {
        viewModelScope.launch {
            try {
                repository.stop(moduleId)
                refresh()
            } catch (t: Throwable) {
                _uiState.update { it.copy(error = "Stop failed: ${t.message}") }
            }
        }
    }

    /** Clear the transient error / success message. */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
