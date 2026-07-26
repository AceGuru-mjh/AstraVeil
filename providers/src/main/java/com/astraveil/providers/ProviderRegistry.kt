package com.astraveil.providers

import com.astraveil.core.event.AstraEvent
import com.astraveil.core.event.EventBus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Event broadcast on the [EventBus] whenever a [RootProvider] becomes the
 * active root backend on this device.
 *
 * @property providerId   The [RootProvider.id] of the now-active backend.
 * @property rootInfo     Snapshot of the backend's [RootInfo] at detection time.
 */
data class ProviderAvailableEvent(
    val providerId: String,
    val rootInfo: RootInfo
) : AstraEvent

/**
 * Central registry of every [RootProvider] known to AstraVeil.
 *
 * Implemented as a process-wide singleton (`object`) so that every subsystem
 * — AstraUI, the SDK facade, the module runtime — can reach the active root
 * backend through `ProviderRegistry.detectActive()` without dependency-injection
 * plumbing. The optional [eventBus] is wired by the application at startup
 * (see `AstraVeilApplication`); when set, a [ProviderAvailableEvent] is
 * published the first time a provider is positively detected.
 *
 * Design notes:
 *  * To add a new backend, append it to [providers] — that is the only edit
 *    required anywhere in the codebase.
 *  * [detectActive] returns the *first* provider whose [RootProvider.available]
 *    probe succeeds. The order in [providers] is therefore the precedence
 *    order: AstraRoot first (so we never accidentally fall through to a legacy
 *    backend once our own exists), then Magisk, KernelSU, APatch.
 *
 * Detection is performed lazily and cached behind a [Mutex] so concurrent
 * callers never cause duplicate `su` round-trips; call [invalidate] to force
 * a re-detect.
 */
object ProviderRegistry {

    /**
     * Optional [EventBus] used to broadcast availability events. The app sets
     * this once during `Application.onCreate`.
     */
    @Volatile
    var eventBus: EventBus? = null

    /**
     * The ordered list of every backend AstraVeil knows how to drive.
     *
     * Order matters: it is the precedence used by [detectActive]. AstraRoot is
     * intentionally first so that, once it ships, it always wins over the
     * legacy backends on devices where both are present.
     */
    private val providers: List<RootProvider> = listOf(
        AstraRootProvider(),
        MagiskProvider(),
        KernelSUProvider(),
        APatchProvider()
    )

    /** Protects [cachedActive] / [cachedAll] from concurrent detection runs. */
    private val mutex = Mutex()

    @Volatile private var cachedActive: RootInfo? = null
    @Volatile private var cachedAll: List<RootInfo>? = null

    /** Return every registered provider (detection is NOT triggered). */
    fun all(): List<RootProvider> = providers

    /** Look up a provider by its [RootProvider.id], or `null` if unknown. */
    fun byId(id: String): RootProvider? = providers.firstOrNull { it.id == id }

    /**
     * Detect and return the first backend that is currently available on this
     * device, or `null` if none is. Results are cached for the lifetime of the
     * registry; call [invalidate] to force a re-detect.
     *
     * A [ProviderAvailableEvent] is emitted the first time a provider is
     * positively detected.
     */
    suspend fun detectActive(): RootInfo? = mutex.withLock {
        cachedActive?.let { return@withLock it }
        for (provider in providers) {
            if (!provider.available()) continue
            val info = provider.detect()
            cachedActive = info
            eventBus?.emit(
                ProviderAvailableEvent(providerId = provider.id, rootInfo = info)
            )
            return@withLock info
        }
        null
    }

    /**
     * Detect every registered provider and return the full [RootInfo] list,
     * including backends that are not currently available (those entries will
     * have [RootInfo.detected] = `false`).
     */
    suspend fun detectAll(): List<RootInfo> = mutex.withLock {
        cachedAll?.let { return@withLock it }
        coroutineScope {
            providers.map { provider ->
                async {
                    if (provider.available()) {
                        provider.detect().also { info ->
                            if (info.detected) {
                                eventBus?.emit(
                                    ProviderAvailableEvent(
                                        providerId = provider.id,
                                        rootInfo = info
                                    )
                                )
                            }
                        }
                    } else {
                        provider.info()
                    }
                }
            }.awaitAll()
        }.also { cachedAll = it }
    }

    /** Drop any cached detection results; the next call re-probes the device. */
    fun invalidate() {
        cachedActive = null
        cachedAll = null
    }

    /**
     * v3: resolve [capability] to the first available provider that
     * advertises it. Returns `null` if no provider is available or no
     * available provider offers the capability.
     *
     * This is the v3 entry point — callers ask "which provider can do
     * MOUNT_NAMESPACE?" instead of "is Magisk present?".
     */
    suspend fun find(capability: ProviderCapability): RootProvider? {
        for (provider in providers) {
            if (provider.available() && capability in provider.capabilities()) {
                return provider
            }
        }
        return null
    }
}
