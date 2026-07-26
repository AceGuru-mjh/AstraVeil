package com.astraveil.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.diagnostics.MvpHealthCheck
import com.astraveil.core.diagnostics.SystemHealthStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the Dashboard with real device + provider data.
 *
 * Exposes a [StateFlow] of [SystemHealthStatus] that the Dashboard
 * collects with `collectAsState()`. Call [refresh] after a provider
 * change or a pull-to-refresh.
 */
class DashboardViewModel(
    private val healthCheck: MvpHealthCheck,
) : ViewModel() {

    private val _state = MutableStateFlow(SystemHealthStatus.offline())
    val state: StateFlow<SystemHealthStatus> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = healthCheck.check()
        }
    }
}
