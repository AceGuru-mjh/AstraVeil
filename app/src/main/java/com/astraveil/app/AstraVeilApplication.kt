package com.astraveil.app

import android.app.Application
import android.util.Log
import com.astraveil.app.ipc.DaemonManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AstraVeilApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "onCreate")
        core = com.astraveil.core.AstraCore(this)
        daemonManager = DaemonManager(this)
        appScope.launch {
            core.initialize()
            com.astraveil.providers.ProviderRegistry.eventBus = core.eventBus
        }

        // Connect to astrad if it's running (started by Magisk service.sh).
        // Non-blocking: retries in background, falls back to local-only mode.
        daemonManager.connectWhenReady()
    }

    companion object {
        private const val TAG = "AstraVeilApp"
        lateinit var instance: AstraVeilApplication
            private set
        lateinit var core: com.astraveil.core.AstraCore
            private set
        lateinit var daemonManager: DaemonManager
            private set
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
