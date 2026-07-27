package com.astraveil.app

import android.app.Application
import android.util.Log
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
        appScope.launch {
            core.initialize()
            com.astraveil.providers.ProviderRegistry.eventBus = core.eventBus
        }
    }

    companion object {
        private const val TAG = "AstraVeilApp"
        lateinit var instance: AstraVeilApplication
            private set
        lateinit var core: com.astraveil.core.AstraCore
            private set
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
