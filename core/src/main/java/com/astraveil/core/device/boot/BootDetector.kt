package com.astraveil.core.device.boot

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BootDetector {

    suspend fun detect(): BootInfo = withContext(Dispatchers.IO) {
        val locked = getProp("ro.boot.flash.locked") ?: "0"
        val verifiedState = getProp("ro.boot.verifiedbootstate") ?: "unknown"
        val vbmetaState = getProp("ro.boot.vbmeta.device_state") ?: "unknown"

        BootInfo(
            unlocked = locked == "0" || vbmetaState == "unlocked",
            verifiedBoot = verifiedState,
            dmVerity = verifiedState == "green" || verifiedState == "yellow",
        )
    }

    private fun getProp(name: String): String? = try {
        val cls = Class.forName("android.os.SystemProperties")
        val method = cls.getMethod("get", String::class.java)
        method.invoke(null, name) as? String
    } catch (t: Throwable) {
        // Fallback: read from build prop via shell
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", "getprop $name"))
            proc.inputStream.bufferedReader().readLine()?.trim()?.takeIf { it.isNotEmpty() }
        } catch (e: Throwable) { null }
    }
}
