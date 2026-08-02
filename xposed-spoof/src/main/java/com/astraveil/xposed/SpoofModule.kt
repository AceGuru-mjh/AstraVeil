package com.astraveil.xposed

import com.astraveil.xposed.hooks.BuildHook
import com.astraveil.xposed.hooks.DetectionHook
import com.astraveil.xposed.hooks.DisplayHook
import com.astraveil.xposed.hooks.SettingsHook
import com.astraveil.xposed.hooks.TelephonyHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject
import java.io.File

class SpoofModule : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        if (pkg == "android" || pkg == "com.astraveil.app" ||
            pkg == "com.astraveil.xposed" || pkg.startsWith("com.android.")
        ) return

        // -- Read shield.json (Environment Shield config) --
        // The shield runs independently of the spoof profile — it hides
        // root/Magisk/Xposed traces even for apps without a spoof profile.
        val shieldEnabled = readShieldEnabled()

        // -- Read spoof profile config --
        val config = ConfigBridge.load(pkg)

        // If neither shield nor spoof is active, skip
        if (!shieldEnabled && !config.enabled) return

        XposedBridge.log("[AstraSpoof] Hooking $pkg (shield=$shieldEnabled, spoof=${config.enabled})")

        // -- Spoof engine hooks (only if spoof profile configured) --
        if (config.enabled) {
            BuildHook.install(config)
            TelephonyHook.install(lpparam.classLoader, config)
            DisplayHook.install(lpparam.classLoader, config)
            SettingsHook.install(lpparam.classLoader, config)
        }

        // -- Environment Shield Java-layer hooks (if shield enabled) --
        // Covers Class.forName/PackageManager/File/Runtime/Build/SystemProperties
        // /Settings/ContentResolver/ApplicationInfo — vectors that cannot be
        // caught at the native file layer.
        if (shieldEnabled) {
            DetectionHook.install(lpparam.classLoader, config)
        }
    }

    /**
     * Reads the "enabled" field from /data/adb/astraveil/shield.json.
     * Returns true if the file is missing (default: shield on).
     */
    private fun readShieldEnabled(): Boolean {
        return try {
            val file = File("/data/adb/astraveil/shield.json")
            if (!file.exists()) return true
            val j = JSONObject(file.readText())
            j.optBoolean("enabled", true)
        } catch (_: Throwable) {
            true  // fail-open: if we can't read the config, enable the shield
        }
    }
}
