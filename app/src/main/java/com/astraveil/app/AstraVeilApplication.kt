package com.astraveil.app

import android.app.Application
import com.astraveil.core.AstraCore
import com.astraveil.core.logger.AstraLogger
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * AstraVeil Application entry point.
 *
 * Owns the process-wide [AstraCore] engine instance. The core is initialised
 * asynchronously on a background [SupervisorJob] scope so the UI can render
 * immediately while capability detection, provider probing and the logger
 * wiring run off the main thread.
 *
 * The reference is exposed as a `lateinit var` on the companion so that
 * ViewModels (which receive this [Application] via [AndroidViewModel]) and
 * Composables can grab the engine without DI ceremony for Phase 0.
 */
class AstraVeilApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Boot the core engine. The constructor wires the appContext; the
        // suspend [AstraCore.initialize] then performs capability probing,
        // provider detection and config loading.
        core = AstraCore(this)

        // Wire the provider registry to the core event bus so that provider
        // availability events flow into AstraUI live.
        ProviderRegistry.eventBus = core.eventBus

        appScope.launch {
            try {
                core.initialize()
                AstraLogger.i(TAG, "AstraCore initialised — AstraVeil ready.")
            } catch (t: Throwable) {
                AstraLogger.e(TAG, "AstraCore init failed", t)
            }
        }
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }

    companion object {
        private const val TAG = "AstraVeilApp"

        @Volatile
        lateinit var instance: AstraVeilApplication
            private set

        /** The global AstraVeil core engine. */
        @Volatile
        lateinit var core: AstraCore
            private set

        /** Process-scoped coroutine context for long-lived app work. */
        val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
