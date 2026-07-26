package com.astraveil.core.device

import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads the real Android device state: SDK, manufacturer, model, kernel,
 * SELinux mode. All probes are non-root — they read `Build.*` and run
 * `getenforce` via a plain shell.
 */
class DeviceInspector {

    fun sdk(): Int = Build.VERSION.SDK_INT

    fun androidVersion(): String = Build.VERSION.RELEASE ?: "unknown"

    fun manufacturer(): String = Build.MANUFACTURER ?: "unknown"

    fun model(): String = Build.MODEL ?: "unknown"

    fun kernel(): String = System.getProperty("os.version") ?: "unknown"

    fun selinux(): String {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("sh", "-c", "getenforce")
            )
            BufferedReader(InputStreamReader(process.inputStream))
                .readLine() ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun inspect(): AndroidProfile = AndroidProfile(
        sdk = sdk(),
        androidVersion = androidVersion(),
        manufacturer = manufacturer(),
        model = model(),
        kernel = kernel(),
        selinux = selinux(),
    )
}
