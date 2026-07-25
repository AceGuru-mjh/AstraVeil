package com.astraveil.core.event

import com.astraveil.core.capability.CapabilityInfo
import com.astraveil.core.logger.LogLevel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Common supertype for every event published on the [EventBus].
 *
 * This is a plain (non-sealed) interface so that other AstraVeil modules —
 * notably `:providers` and `:modules` — can declare their own event subtypes
 * in their own packages without forcing a cyclic dependency back onto the
 * core module. Core ships the baseline catalogue below; consumers may add
 * additional `data class`es implementing [AstraEvent] freely.
 */
interface AstraEvent

/**
 * Capability snapshot for the device changed (e.g. after a rescan).
 *
 * @property info The new [CapabilityInfo].
 */
data class CapabilityUpdatedEvent(val info: CapabilityInfo) : AstraEvent

/**
 * A root provider transitioned to the online state.
 *
 * @property providerName Display name of the provider (e.g. "magisk").
 */
data class ProviderOnlineEvent(val providerName: String) : AstraEvent

/**
 * A root provider transitioned to the offline state.
 *
 * @property providerName Display name of the provider.
 */
data class ProviderOfflineEvent(val providerName: String) : AstraEvent

/**
 * A permission was granted to a module.
 *
 * @property moduleId   Identifier of the module receiving the grant.
 * @property permission Permission name that was granted.
 */
data class PermissionGrantedEvent(val moduleId: String, val permission: String) : AstraEvent

/**
 * A permission was revoked from a module.
 *
 * @property moduleId   Identifier of the module losing the permission.
 * @property permission Permission name that was revoked.
 */
data class PermissionRevokedEvent(val moduleId: String, val permission: String) : AstraEvent

/**
 * A module was installed.
 *
 * @property moduleId Identifier of the newly installed module.
 * @property version  Optional version string of the installed module.
 */
data class ModuleInstalledEvent(
    val moduleId: String,
    val version: String = ""
) : AstraEvent

/**
 * A module was uninstalled.
 *
 * @property moduleId Identifier of the removed module.
 */
data class ModuleUninstalledEvent(val moduleId: String) : AstraEvent

/**
 * A module transitioned between lifecycle states.
 *
 * @property moduleId Identifier of the module that changed.
 * @property state    Human-readable target state name (e.g. "RUNNING").
 */
data class ModuleStateChangedEvent(
    val moduleId: String,
    val state: String
) : AstraEvent

/**
 * A security violation was detected (e.g. a sandbox escape attempt or a
 * signature mismatch). The UI treats this as an alarm condition.
 *
 * @property reason Short human-readable description of the violation.
 */
data class SecurityViolationEvent(val reason: String) : AstraEvent

/**
 * A log entry was produced.
 *
 * @property level   Severity of the entry.
 * @property tag     Logger tag.
 * @property message Message body.
 */
data class LogEvent(val level: LogLevel, val tag: String, val message: String) : AstraEvent

/**
 * Application-wide event bus implemented as a [MutableSharedFlow].
 *
 * The bus has no replay (new subscribers only see events emitted after they
 * subscribe) and a buffer capacity of 64. When the buffer overflows the
 * oldest events are dropped, ensuring publishers never block subscribers and
 * vice versa.
 *
 * Both coroutine and non-coroutine callers are supported: use [emit] from a
 * `suspend` context, or [tryEmit] when calling from a synchronous code path
 * such as the [com.astraveil.core.permission.PermissionEngine] callbacks.
 */
object EventBus {

    private val flow = MutableSharedFlow<AstraEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Publish [event] to all current subscribers. With the configured
     * [BufferOverflow.DROP_OLDEST] policy this returns immediately even when
     * the buffer is full.
     */
    suspend fun emit(event: AstraEvent) {
        flow.emit(event)
    }

    /**
     * Non-suspending best-effort publish.
     *
     * Useful when the caller is not in a coroutine (e.g. permission engine
     * handlers). Returns `true` if the event was accepted into the buffer,
     * `false` if it was dropped due to overflow.
     */
    fun tryEmit(event: AstraEvent): Boolean = flow.tryEmit(event)

    /**
     * Hot flow of every published [AstraEvent]. Subscribers cancel their
     * collector coroutine to stop receiving events.
     */
    val events: Flow<AstraEvent> = flow.asSharedFlow()

    /**
     * Convenience filter that emits only events of type [T].
     *
     * Example:
     * ```
     * eventBus.eventsOf<PermissionGrantedEvent>().collect { ev -> ... }
     * ```
     */
    inline fun <reified T : AstraEvent> eventsOf(): Flow<T> =
        flow.asSharedFlow().filterIsInstance()
}
