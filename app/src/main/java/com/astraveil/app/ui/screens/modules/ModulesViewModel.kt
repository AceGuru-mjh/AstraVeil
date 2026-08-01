package com.astraveil.app.ui.screens.modules

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.repository.ModuleRepositoryProvider
import com.astraveil.app.ui.AstraStrings
import com.astraveil.app.repository.ScanResult
import com.astraveil.core.modules.model.ModuleInfo
import com.astraveil.core.modules.security.TrustReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Unified operation feedback state for any module action
 * (install / start / stop / uninstall). Patch 18.2.3.
 */
sealed class ModuleOperationState {
    data object Idle : ModuleOperationState()
    data object Loading : ModuleOperationState()
    data class Success(val message: String) : ModuleOperationState()
    data class Error(val message: String) : ModuleOperationState()
}

/**
 * UI state for the Module Center screen.
 */
data class ModulesUiState(
    val modules: List<ModuleInfo> = emptyList(),
    val loading: Boolean = false,
    val installState: ModuleOperationState = ModuleOperationState.Idle,
    val moduleOperations: Map<String, ModuleOperationState> = emptyMap(),
)

/**
 * State machine for the install-preview flow (PR18.3: now carries a
 * [TrustReport], not just a manifest preview).
 *
 * ```
 * IDLE → PREVIEWING → READY (security review dialog shown)
 *                    → FAILED (error snackbar)
 * READY → (confirmInstall) → installState.Loading → IDLE (success/error)
 * ```
 */
sealed class PreviewState {
    /** No file selected, no dialog. */
    data object Idle : PreviewState()

    /** Scanning the .avm: hashing + manifest parse + risk analysis. */
    data object Previewing : PreviewState()

    /** Trust report ready. Security review dialog should show [report]. */
    data class Ready(
        val report: TrustReport,
        val uri: Uri,
    ) : PreviewState()

    /** Scan failed. Show [reason] to the user. */
    data class Failed(val reason: String) : PreviewState()
}

/**
 * ViewModel for the AVM Module Center.
 *
 * Data flow (PR18.3):
 * ```
 * ModulesScreen → ModulesViewModel → ModuleRepository → ModuleScanner
 *                                       └─ HashCalculator + AvmManifestParser
 *                                          + RiskAnalyzer → TrustReport
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
                            AstraStrings.loadModulesFailed(t.message)
                        ),
                    )
                }
            }
        }
    }

    /**
     * Step 1 of install flow: run the Trust Pipeline on the .avm.
     *
     * Computes SHA-256, parses the manifest, and produces a [TrustReport].
     * Does NOT install anything.
     */
    fun previewUri(uri: Uri) {
        viewModelScope.launch {
            _previewState.value = PreviewState.Previewing
            when (val result = repository.preview(uri)) {
                is ScanResult.Success -> {
                    _previewState.value = PreviewState.Ready(
                        report = result.report,
                        uri = uri,
                    )
                }
                is ScanResult.Failure -> {
                    _previewState.value = PreviewState.Failed(
                        reason = result.reason,
                    )
                }
            }
        }
    }

    /**
     * Step 2 of install flow: user confirmed in the security review dialog.
     * Actually installs the .avm via ModuleManager.
     */
    fun confirmInstall() {
        val state = _previewState.value
        if (state !is PreviewState.Ready) return

        viewModelScope.launch {
            _uiState.update { it.copy(installState = ModuleOperationState.Loading) }
            try {
                val installed = repository.install(
                    uri = state.uri,
                    expectedHash = state.report.packageHash,
                )
                _uiState.update {
                    it.copy(
                        installState = ModuleOperationState.Success(
                            AstraStrings.installSuccess(installed.name)
                        ),
                        modules = it.modules + installed,
                    )
                }
                _previewState.value = PreviewState.Idle
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        installState = ModuleOperationState.Error(
                            AstraStrings.installFailed(t.message)
                        ),
                    )
                }
                _previewState.value = PreviewState.Idle
            }
        }
    }

    /** Dismiss the security review dialog without installing. */
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
                    it.copy(installState = ModuleOperationState.Success(AstraStrings.modUninstalled))
                }
            } catch (t: Throwable) {
                setModuleOp(
                    moduleId,
                    ModuleOperationState.Error(AstraStrings.uninstallFailed(t.message)),
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
                setModuleOp(moduleId, ModuleOperationState.Success(AstraStrings.modStarted))
                refresh()
            } catch (t: Throwable) {
                setModuleOp(
                    moduleId,
                    ModuleOperationState.Error(AstraStrings.startFailed(t.message)),
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
                setModuleOp(moduleId, ModuleOperationState.Success(AstraStrings.modStopped))
                refresh()
            } catch (t: Throwable) {
                setModuleOp(
                    moduleId,
                    ModuleOperationState.Error(AstraStrings.stopFailed(t.message)),
                )
            }
        }
    }

    fun clearInstallState() {
        _uiState.update { it.copy(installState = ModuleOperationState.Idle) }
    }

    fun clearModuleOp(moduleId: String) {
        _uiState.update { it.copy(moduleOperations = it.moduleOperations - moduleId) }
    }

    private fun setModuleOp(moduleId: String, state: ModuleOperationState) {
        _uiState.update {
            it.copy(moduleOperations = it.moduleOperations + (moduleId to state))
        }
    }
}
