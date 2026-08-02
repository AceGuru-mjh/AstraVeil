package com.astraveil.app.spoof

import android.content.Context
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Environment Shield configuration manager.
 *
 * Writes /data/adb/astraveil/shield.json which the Zygisk module reads
 * at preAppSpecialize. The LSPosed module reads the same file via
 * ConfigBridge.
 *
 * Reasoning: the Zygisk module runs with the target app's UID and can
 * only read files — it cannot access the manager's DataStore. File IPC
 * is the only reliable channel.
 */
object EnvShieldManager {

    private const val CONFIG_PATH = "/data/adb/astraveil/shield.json"

    data class ShieldState(
        val enabled: Boolean = true,
        val hideRoot: Boolean = true,
        val hideMagisk: Boolean = true,
        val hideXposed: Boolean = true,
        val hideMounts: Boolean = true,
        val hideMaps: Boolean = true,
        val hideSelinux: Boolean = true,
        val hideDebugger: Boolean = true,
        val hideFrida: Boolean = true,
        val hideNetUnix: Boolean = true,
        // Per-tool bypass
        val momoBypass: Boolean = false,
        val ruruBypass: Boolean = false,
        val chunqiuBypass: Boolean = false,
        val hunterBypass: Boolean = false,
    )

    suspend fun writeConfig(context: Context, state: ShieldState) {
        withContext(Dispatchers.IO) {
            val provider = ProviderRegistry.activeProvider() ?: return@withContext

            val json = JSONObject().apply {
                put("enabled", state.enabled)
                put("hide_root", state.hideRoot)
                put("hide_magisk", state.hideMagisk)
                put("hide_xposed", state.hideXposed)
                put("hide_mounts", state.hideMounts)
                put("hide_maps", state.hideMaps)
                put("hide_selinux", state.hideSelinux)
                put("hide_debugger", state.hideDebugger)
                put("hide_frida", state.hideFrida)
                put("hide_net_unix", state.hideNetUnix)
                put("shield_momo", state.momoBypass.toString())
                put("shield_ruru", state.ruruBypass.toString())
                put("shield_chunqiu", state.chunqiuBypass.toString())
                put("shield_hunter", state.hunterBypass.toString())
            }

            val escaped = json.toString().replace("'", "'\\''")
            @Suppress("DEPRECATION")
            provider.execute("mkdir -p /data/adb/astraveil")
            @Suppress("DEPRECATION")
            provider.execute("echo '$escaped' > $CONFIG_PATH")
            @Suppress("DEPRECATION")
            provider.execute("chmod 0644 $CONFIG_PATH")
        }
    }

    suspend fun readConfig(context: Context): ShieldState {
        return withContext(Dispatchers.IO) {
            runCatching {
                val provider = ProviderRegistry.activeProvider()
                    ?: return@withContext ShieldState()
                @Suppress("DEPRECATION")
                val raw = provider.execute("cat $CONFIG_PATH 2>/dev/null").stdout.trim()
                if (raw.isEmpty()) return@withContext ShieldState()

                val j = JSONObject(raw)
                ShieldState(
                    enabled = j.optBoolean("enabled", true),
                    hideRoot = j.optBoolean("hide_root", true),
                    hideMagisk = j.optBoolean("hide_magisk", true),
                    hideXposed = j.optBoolean("hide_xposed", true),
                    hideMounts = j.optBoolean("hide_mounts", true),
                    hideMaps = j.optBoolean("hide_maps", true),
                    hideSelinux = j.optBoolean("hide_selinux", true),
                    hideDebugger = j.optBoolean("hide_debugger", true),
                    hideFrida = j.optBoolean("hide_frida", true),
                    hideNetUnix = j.optBoolean("hide_net_unix", true),
                    momoBypass = j.optString("shield_momo") == "true",
                    ruruBypass = j.optString("shield_ruru") == "true",
                    chunqiuBypass = j.optString("shield_chunqiu") == "true",
                    hunterBypass = j.optString("shield_hunter") == "true",
                )
            }.getOrDefault(ShieldState())
        }
    }
}
