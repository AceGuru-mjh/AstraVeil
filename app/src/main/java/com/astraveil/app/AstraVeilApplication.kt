package com.astraveil.app

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * AstraVeil Application entry point.
 *
 * Ultra crash-safe: everything is wrapped in try-catch. The app NEVER
 * crashes from Application.onCreate — if core init fails, the UI shows
 * defaults.
 */
class AstraVeilApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "AstraVeilApplication.onCreate")

        // Boot the core engine in background. Every step is guarded.
        appScope.launch {
            try {
                val engine = com.astraveil.core.AstraCore(this@AstraVeilApplication)
                core = engine
                Log.i(TAG, "AstraCore constructed")

                try {
                    engine.initialize()
                    Log.i(TAG, "AstraCore initialized")
                } catch (t: Throwable) {
                    Log.e(TAG, "AstraCore init failed", t)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "AstraCore construction failed", t)
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

        @Volatile
        var core: com.astraveil.core.AstraCore? = null
            private set

        val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
