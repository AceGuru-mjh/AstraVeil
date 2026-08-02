package com.astraveil.app.modules

import android.content.Context
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Built-in module state management.
 *
 * Persistence path: /data/adb/astraveil/shield.json
 * Format is consistent with what the Zygisk module reads.
 *
 * Reasoning: we don't use DataStore/MMKV because the Zygisk module
 * runs with the target app's UID and can only read files — it cannot
 * access the manager's DataStore. File IPC is the only reliable channel.
 */
object BuiltinModuleManager {

    private const val CONFIG_DIR = "/data/adb/astraveil"
    private const val SHIELD_PATH = "$CONFIG_DIR/shield.json"

    /** Read the enabled state of all built-in modules. */
    suspend fun loadStates(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val provider = ProviderRegistry.activeProvider()
                ?: return@withContext defaultStates()
            @Suppress("DEPRECATION")
            val raw = provider.execute("cat $SHIELD_PATH 2>/dev/null").stdout.trim()
            if (raw.isEmpty() || raw == "null") return@withContext defaultStates()

            val j = JSONObject(raw)
            mapOf(
                "shield_core" to j.optBoolean("enabled", true),
                "spoof_engine" to j.optBoolean("spoof_enabled", true),
                "bypass_momo" to (j.optString("shield_momo") == "true"),
                "bypass_ruru" to (j.optString("shield_ruru") == "true"),
                "bypass_chunqiu" to (j.optString("shield_chunqiu") == "true"),
                "bypass_hunter" to (j.optString("shield_hunter") == "true"),
            )
        }.getOrDefault(defaultStates())
    }

    /** Toggle a single built-in module. */
    suspend fun setEnabled(context: Context, moduleId: String, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            val provider = ProviderRegistry.activeProvider() ?: return@withContext

            // Read current config
            @Suppress("DEPRECATION")
            val raw = provider.execute("cat $SHIELD_PATH 2>/dev/null").stdout.trim()
            val j = if (raw.isNotEmpty() && raw != "null") {
                runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
            } else {
                JSONObject()
            }

            // Update the corresponding field
            when (moduleId) {
                "shield_core" -> {
                    j.put("enabled", enabled)
                    // Core shield controls all general hide switches
                    j.put("hide_root", enabled)
                    j.put("hide_magisk", enabled)
                    j.put("hide_xposed", enabled)
                    j.put("hide_mounts", enabled)
                    j.put("hide_maps", enabled)
                    j.put("hide_selinux", enabled)
                    j.put("hide_debugger", enabled)
                    j.put("hide_frida", enabled)
                    j.put("hide_net_unix", enabled)
                }
                "spoof_engine" -> j.put("spoof_enabled", enabled)
                "bypass_momo" -> j.put("shield_momo", enabled.toString())
                "bypass_ruru" -> j.put("shield_ruru", enabled.toString())
                "bypass_chunqiu" -> j.put("shield_chunqiu", enabled.toString())
                "bypass_hunter" -> j.put("shield_hunter", enabled.toString())
            }

            // Write back
            val escaped = j.toString().replace("'", "'\\''")
            @Suppress("DEPRECATION")
            provider.execute("mkdir -p $CONFIG_DIR")
            @Suppress("DEPRECATION")
            provider.execute("echo '$escaped' > $SHIELD_PATH")
            @Suppress("DEPRECATION")
            provider.execute("chmod 0644 $SHIELD_PATH")
        }
    }

    private fun defaultStates(): Map<String, Boolean> = mapOf(
        "shield_core" to true,
        "spoof_engine" to true,
        "bypass_momo" to false,
        "bypass_ruru" to false,
        "bypass_chunqiu" to false,
        "bypass_hunter" to false,
    )
}
