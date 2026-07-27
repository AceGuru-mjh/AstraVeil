package com.astraveil.app

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class AstraVeilApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "onCreate")
    }

    companion object {
        private const val TAG = "AstraVeilApp"
        lateinit var instance: AstraVeilApplication
            private set
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
