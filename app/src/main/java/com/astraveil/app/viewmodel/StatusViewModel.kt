package com.astraveil.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.BuildConfig
import com.astraveil.core.capability.CapabilityInfo
import com.astraveil.providers.RootInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusViewModel(app: Application) : AndroidViewModel(app) {

    enum class DaemonStatus { OFFLINE, CONNECTING, ONLINE }

    data class RootTestResult(
        val success: Boolean,
        val output: String,
        val providerName: String,
        val error: String? = null,
    )

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
        val lastError: String? = null,
        val rootTestResult: RootTestResult? = null,
        val rootTesting: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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

    /**
     * Test root capability by executing `id` through the active provider.
     * On success: output contains "uid=0(root)".
     * On failure: error message explains why.
     */
    fun testRootCapability() {
        viewModelScope.launch {
            _uiState.update { it.copy(rootTesting = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val info = runCatching {
                        com.astraveil.providers.ProviderRegistry.detectActive()
                    }.getOrNull()

                    if (info == null) {
                        return@withContext RootTestResult(
                            success = false,
                            output = "",
                            providerName = "None",
                            error = "No root provider detected",
                        )
                    }

                    val provider = com.astraveil.providers.ProviderRegistry
                        .byId(info.providerName)

                    if (provider == null || !provider.available()) {
                        return@withContext RootTestResult(
                            success = false,
                            output = "",
                            providerName = info.displayName,
                            error = "Provider not available",
                        )
                    }

                    // Execute `id` via the provider.
                    val execResult = runCatching {
                        provider.execute("id")
                    }.getOrNull()

                    if (execResult == null || !execResult.success) {
                        return@withContext RootTestResult(
                            success = false,
                            output = execResult?.stdout ?: "",
                            providerName = provider.displayName,
                            error = execResult?.stderr ?: "Execution failed",
                        )
                    }

                    RootTestResult(
                        success = true,
                        output = execResult.stdout.trim(),
                        providerName = provider.displayName,
                    )
                }

                _uiState.update { it.copy(rootTesting = false, rootTestResult = result) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        rootTesting = false,
                        rootTestResult = RootTestResult(
                            success = false,
                            output = "",
                            providerName = "Error",
                            error = t.message,
                        )
                    )
                }
            }
        }
    }
}
