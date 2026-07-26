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
 * Holds the dashboard-wide UI state for AstraUI.
 *
 * The VM reads from the global [AstraVeilApplication.core] engine, polls
 * [ProviderRegistry] for the active root backend, and subscribes to the
 * core [EventBus] for live updates. UI state is exposed as a single
 * [StateFlow] of [UiState] to keep recomposition cheap.
 */
class StatusViewModel(app: Application) : AndroidViewModel(app) {

    /** Lifecycle state of the AstraVeil daemon (Phase 0: always OFFLINE). */
    enum class DaemonStatus { OFFLINE, CONNECTING, ONLINE }

    /**
     * Immutable snapshot of everything AstraUI renders.
     */
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
        observeEvents()
    }

    /**
     * Re-probe capability + provider. Sets [UiState.scanning] while running
     * so the UI can show a progress affordance. Always runs on Dispatchers.IO
     * (the core implementations are blocking).
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(scanning = true, lastError = null) }
            try {
                val core = AstraVeilApplication.core
                runCatching { core.refreshCapability() }
                    .onFailure { AstraLogger.w(TAG, "refreshCapability failed: ${it.message}") }

                val capability = runCatching { core.capability }
                    .getOrNull()
                    ?: CapabilityInfo.empty()

                val detected = runCatching { ProviderRegistry.detectActive() }
                    .getOrNull()

                _uiState.update { current ->
                    current.copy(
                        scanning = false,
                        capability = capability,
                        daemonStatus = DaemonStatus.OFFLINE, // Phase 0: no daemon yet
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

    /**
     * Subscribe to the core [EventBus] and reflect interesting events
     * (capability changes, provider switch, security warnings) into [UiState].
     */
    private fun observeEvents() {
        viewModelScope.launch {
            EventBus.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: AstraEvent) {
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
            else -> { /* other events ignored at this layer */ }
        }
    }

    private companion object {
        private const val TAG = "StatusViewModel"
    }
}
