package com.astraveil.app.hub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class HubUiState(
    val loading: Boolean = false,
    val modules: List<HubModule> = emptyList(),
    val filtered: List<HubModule> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val downloadingId: String? = null,
    val pendingInstallPath: String? = null,
)

class AstraHubViewModel(app: Application) : AndroidViewModel(app) {

    private val client = AstraHubClient()
    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    private var index: AstraHubIndex = AstraHubIndex(schemaVersion = 1)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                index = client.fetchIndex()
                _uiState.update {
                    it.copy(loading = false, modules = index.modules,
                        filtered = index.modules)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false,
                    error = e.message ?: "Failed to load AstraHub") }
            }
        }
    }

    fun search(query: String) {
        _uiState.update {
            it.copy(searchQuery = query,
                filtered = client.search(index, query))
        }
    }

    /**
     * Download + verify SHA-256. On success, exposes the local file path
     * via [HubUiState.pendingInstallPath] so the UI can hand it off to
     * ModuleRepository.install (which enforces TrustGate + capability
     * compatibility). AstraHub only does discovery + transport integrity.
     */
    fun install(module: HubModule) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingId = module.id, pendingInstallPath = null) }
            try {
                val cacheDir = File(getApplication<Application>().cacheDir, "hub")
                val file = client.downloadVerified(module, cacheDir)
                if (file == null) {
                    _uiState.update {
                        it.copy(downloadingId = null,
                            error = "Download or checksum verification failed for ${module.id}.")
                    }
                } else {
                    _uiState.update {
                        it.copy(downloadingId = null, pendingInstallPath = file.absolutePath)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(downloadingId = null,
                        error = e.message ?: "Download failed")
                }
            }
        }
    }

    fun consumePendingInstallPath() {
        _uiState.update { it.copy(pendingInstallPath = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
