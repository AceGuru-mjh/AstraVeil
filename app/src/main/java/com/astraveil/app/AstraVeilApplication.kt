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
 * Crash-safe: the core engine is constructed inside a try-catch so a
 * failure during AstraCore construction does not crash the process.
 * The `core` lateinit is only set when construction succeeds; the
 * StatusViewModel guards its access with runCatching.
 */
class AstraVeilApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Boot the core engine. Wrapped in try-catch so a failure does
        // not crash the app — the UI shows "offline" state instead.
        try {
            val engine = AstraCore(this)
            core = engine

            // Wire the provider registry to the core event bus.
            ProviderRegistry.eventBus = engine.eventBus

            appScope.launch {
                try {
                    engine.initialize()
                    AstraLogger.i(TAG, "AstraCore initialised — AstraVeil ready.")
                } catch (t: Throwable) {
                    AstraLogger.e(TAG, "AstraCore init failed", t)
                }
            }
        } catch (t: Throwable) {
            AstraLogger.e(TAG, "AstraCore construction failed", t)
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

        /**
         * The global AstraVeil core engine.
         *
         * Set during [onCreate]. Callers MUST guard access with
         * `runCatching { AstraVeilApplication.core }` because the
         * lateinit may not be set if construction failed.
         */
        @Volatile
        lateinit var core: AstraCore
            private set

        val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
