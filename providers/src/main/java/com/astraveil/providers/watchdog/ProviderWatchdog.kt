package com.astraveil.providers.watchdog

import com.astraveil.core.event.AstraEvent
import com.astraveil.core.event.EventBus
import com.astraveil.core.logger.AstraLogger
import com.astraveil.providers.ProviderRegistry
import com.astraveil.providers.RootProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Live Provider Hot-Swap watchdog.
 *
 * Periodically probes all providers. When the active provider disappears
 * (e.g. user uninstalled Magisk and installed KernelSU), automatically
 * hot-swaps to the next available provider without requiring an app restart.
 *
 * Similar to Linux network bonding failover — when eth0 goes down, eth1
 * takes over automatically.
 */
class ProviderWatchdog(
    private val registry: ProviderRegistry,
    private val eventBus: EventBus,
    private val scope: CoroutineScope,
    private val probeIntervalMs: Long = 30_000L,
) {
    data class Status(
        val activeProviderId: String = "none",
        val availableProviders: List<String> = emptyList(),
        val lastProbeMs: Long = 0,
        val switchCount: Int = 0,
        val healthy: Boolean = false,
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    private var watchdogJob: Job? = null
    private var switchCount = 0

    @Volatile
    private var currentActiveId: String = "none"

    fun start() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                probe()
                delay(probeIntervalMs)
            }
        }
        AstraLogger.i(TAG, "ProviderWatchdog started (interval=${probeIntervalMs}ms)")
    }

    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
        AstraLogger.i(TAG, "ProviderWatchdog stopped")
    }

    private suspend fun probe() {
        val providers = registry.all()
        val available = mutableListOf<String>()
        var activeStillAlive = false

        // Track current active via suspend call
        val activeProvider = registry.activeProvider()
        currentActiveId = activeProvider?.id ?: "none"

        for (provider in providers) {
            val isAvailable = withContext(Dispatchers.IO) {
                runCatching { provider.available() }.getOrDefault(false)
            }
            if (isAvailable) {
                available.add(provider.id)
                if (provider.id == currentActiveId) {
                    activeStillAlive = true
                }
            }
        }

        if (!activeStillAlive && currentActiveId != "none") {
            AstraLogger.w(TAG, "Active provider '$currentActiveId' lost! " +
                "Available: $available. Attempting hot-swap...")
            eventBus.emit(ProviderEvent.Switching(currentActiveId, available))

            if (available.isNotEmpty()) {
                val newActive = providers.firstOrNull { it.id in available }
                if (newActive != null) {
                    // Invalidate cache so detectActive picks up the new provider
                    registry.invalidate()
                    switchCount++
                    AstraLogger.i(TAG, "Hot-swapped to '${newActive.id}' " +
                        "(switch #$switchCount)")
                    eventBus.emit(ProviderEvent.Switched(currentActiveId, newActive.id))
                    currentActiveId = newActive.id
                }
            } else {
                AstraLogger.e(TAG, "No providers available! Root capability lost.", null)
                eventBus.emit(ProviderEvent.AllLost(currentActiveId))
            }
        }

        _status.value = Status(
            activeProviderId = currentActiveId,
            availableProviders = available,
            lastProbeMs = System.currentTimeMillis(),
            switchCount = switchCount,
            healthy = available.isNotEmpty(),
        )
    }

    companion object {
        private const val TAG = "ProviderWatchdog"
    }
}

sealed class ProviderEvent : AstraEvent {
    data class Switching(val from: String, val available: List<String>) : ProviderEvent()
    data class Switched(val from: String, val to: String) : ProviderEvent()
    data class AllLost(val lastActive: String) : ProviderEvent()
}
