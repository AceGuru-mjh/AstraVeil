package com.astraveil.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.AstraVeilApplication
import com.astraveil.app.BuildConfig
import com.astraveil.core.capability.CapabilityInfo
import com.astraveil.core.event.AstraEvent
import com.astraveil.core.event.CapabilityUpdatedEvent
import com.astraveil.core.event.EventBus
import com.astraveil.core.event.SecurityViolationEvent
import com.astraveil.core.logger.AstraLogger
import com.astraveil.providers.ProviderAvailableEvent
import com.astraveil.providers.ProviderRegistry
import com.astraveil.providers.RootInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Crash-safe dashboard ViewModel.
 *
 * Uses nullable `core` instead of lateinit to avoid
 * UninitializedPropertyAccessException. Every call is guarded.
 */
class StatusViewModel(app: Application) : AndroidViewModel(app) {

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
        viewModelScope.launch {
            // Small delay to ensure Application.onCreate has run.
            kotlinx.coroutines.delay(100)
            refresh()
        }
        observeEvents()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(scanning = true, lastError = null) }
            try {
                val core = AstraVeilApplication.core
                if (core != null) {
                    runCatching { core.refreshCapability() }
                        .onFailure { AstraLogger.w(TAG, "refreshCapability failed: ${it.message}") }
                }

                val capability = core?.let {
                    runCatching { it.capability }.getOrNull()
                } ?: CapabilityInfo.empty()

                val detected = runCatching { ProviderRegistry.detectActive() }
                    .getOrNull()

                _uiState.update { current ->
                    current.copy(
                        scanning = false,
                        capability = capability,
                        daemonStatus = DaemonStatus.OFFLINE,
                        providerName = detected?.displayName ?: "None",
                        providerVersion = detected?.version ?: "—",
                        providerInfo = detected,
                        securityProtected = true
                    )
                }
                AstraLogger.i(TAG, "Refresh complete — provider=${detected?.providerName ?: "none"}")
            } catch (t: Throwable) {
                AstraLogger.e(TAG, "Refresh failed", t)
                _uiState.update {
                    it.copy(scanning = false, lastError = t.message ?: t.javaClass.simpleName)
                }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            try {
                EventBus.events.collect { event ->
                    handleEvent(event)
                }
            } catch (t: Throwable) {
                AstraLogger.e(TAG, "EventBus collect failed", t)
            }
        }
    }

    private fun handleEvent(event: AstraEvent) {
        try {
            when (event) {
                is CapabilityUpdatedEvent -> _uiState.update {
                    it.copy(capability = event.info)
                }
                is ProviderAvailableEvent -> _uiState.update {
                    it.copy(
                        providerName = event.rootInfo.displayName,
                        providerVersion = event.rootInfo.version,
                        providerInfo = event.rootInfo
                    )
                }
                is SecurityViolationEvent -> _uiState.update {
                    it.copy(securityProtected = false)
                }
                else -> {}
            }
        } catch (t: Throwable) {
            AstraLogger.e(TAG, "handleEvent failed", t)
        }
    }

    private companion object {
        private const val TAG = "StatusViewModel"
    }
}
