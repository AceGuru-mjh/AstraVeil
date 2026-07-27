package com.astraveil.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.BuildConfig
import com.astraveil.core.capability.CapabilityInfo
import com.astraveil.providers.RootInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StatusViewModel : ViewModel() {

    enum class DaemonStatus { OFFLINE, CONNECTING, ONLINE }

    data class UiState(
        val coreVersion: String = BuildConfig.ASTRAVEIL_VERSION,
        val daemonStatus: DaemonStatus = DaemonStatus.OFFLINE,
        val providerName: String = "None",
        val providerVersion: String = "—",
        val providerInfo: RootInfo? = null,
        val capability: CapabilityInfo = CapabilityInfo.empty(),
        val modulesActive: Int = 0,
        val scanning: Boolean = false,
        val securityProtected: Boolean = true,
        val lastError: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(scanning = true, lastError = null) }
            try {
                val capability = runCatching {
                    com.astraveil.core.capability.CapabilityEngine().scan()
                }.getOrNull() ?: CapabilityInfo.empty()

                val detected = runCatching {
                    com.astraveil.providers.ProviderRegistry.detectActive()
                }.getOrNull()

                _uiState.update { current ->
                    current.copy(
                        scanning = false,
                        capability = capability,
                        providerName = detected?.displayName ?: "None",
                        providerVersion = detected?.version ?: "—",
                        providerInfo = detected,
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(scanning = false, lastError = t.message)
                }
            }
        }
    }
}
