package com.astraveil.app.adb

import android.content.Context
import android.provider.Settings
import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AdbManager {

    private const val TAG = "AdbManager"
    const val SHELL_UID = 2000

    data class AdbStatus(
        val enabled: Boolean,
        val daemonRunning: Boolean,
        val rootMode: Boolean,
        val tcpPort: Int,
        val shellUid: Int = SHELL_UID,
    )

    suspend fun detect(context: Context): AdbStatus = withContext(Dispatchers.IO) {
        val enabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) { false }

        val daemonRunning = try {
            val proc = ProcessBuilder("pidof", "adbd").start()
            val output = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            output.isNotBlank()
        } catch (e: Exception) { false }

        val tcpPort = try {
            val proc = ProcessBuilder("getprop", "service.adb.tcp.port").start()
            val output = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            output.toIntOrNull() ?: -1
        } catch (e: Exception) { -1 }

        val rootMode = try {
            val proc = ProcessBuilder("sh", "-c", "ps -A 2>/dev/null | grep adbd").start()
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            output.contains("root")
        } catch (e: Exception) { false }

        AdbStatus(enabled = enabled, daemonRunning = daemonRunning, rootMode = rootMode, tcpPort = tcpPort)
    }

    fun buildAdbShellCommand(command: String, hasRoot: Boolean): String {
        return if (hasRoot) {
            "su $SHELL_UID sh -c '${command.replace("'", "'\\''")}'"
        } else {
            command
        }
    }

    val quickCommands = listOf(
        "getprop ro.build.display.id",
        "getprop ro.build.version.release",
        "pm list packages -3",
        "dumpsys battery",
        "wm size",
        "wm density",
        "settings get global adb_enabled",
        "logcat -d -t 10",
        "cat /proc/cpuinfo | head -10",
        "df -h /data",
    )
}
