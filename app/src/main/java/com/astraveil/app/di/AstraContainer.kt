package com.astraveil.app.di

import com.astraveil.app.diagnostics.MvpHealthCheck
import com.astraveil.core.device.DeviceRepository
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AstraContainer {

    val deviceRepository: DeviceRepository by lazy { DeviceRepository() }
    val providerRegistry: ProviderRegistry by lazy { ProviderRegistry }
    val healthCheck: MvpHealthCheck by lazy { MvpHealthCheck(providerRegistry) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize() {
        scope.launch { deviceRepository.refresh() }
    }
}
