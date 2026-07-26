package com.astraveil.app.di

import com.astraveil.app.diagnostics.MvpHealthCheck
import com.astraveil.core.device.DeviceRepository
import com.astraveil.providers.ProviderRegistry

/**
 * Lightweight service-locator container.
 *
 * Avoids pulling in Hilt/Koin for the MVP — every subsystem is
 * constructed lazily here and accessed via the [container] object.
 * Migration to Hilt can happen in a later phase without changing
 * call sites.
 */
object AstraContainer {

    val deviceRepository: DeviceRepository by lazy { DeviceRepository() }

    val providerRegistry: ProviderRegistry by lazy { ProviderRegistry }

    val healthCheck: MvpHealthCheck by lazy { MvpHealthCheck(providerRegistry) }

    fun initialize() {
        // Eagerly touch the singletons so they are ready before the UI.
        deviceRepository.refresh()
    }
}
