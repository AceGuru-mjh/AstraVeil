package com.astraveil.core.lifecycle

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Application-wide lifecycle events.
 *
 * Emitted by the daemon, module runtime, and recovery manager;
 * collected by the UI to update the dashboard health badges.
 */
sealed class LifecycleEvent {
    data object DaemonStarted : LifecycleEvent()
    data object DaemonStopped : LifecycleEvent()
    data class ModuleCrashed(val id: String) : LifecycleEvent()
    data class ModuleStarted(val id: String) : LifecycleEvent()
    data class ModuleStopped(val id: String) : LifecycleEvent()
    data class RecoveryTriggered(val reason: String) : LifecycleEvent()
}

/**
 * Broadcasts [LifecycleEvent]s to every subscriber.
 */
class LifecycleManager {

    private val _events = MutableSharedFlow<LifecycleEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<LifecycleEvent> = _events.asSharedFlow()

    suspend fun emit(event: LifecycleEvent) {
        _events.emit(event)
    }
}
