package com.astraveil.xposed

import com.astraveil.xposed.hooks.BuildHook
import com.astraveil.xposed.hooks.DisplayHook
import com.astraveil.xposed.hooks.SettingsHook
import com.astraveil.xposed.hooks.TelephonyHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SpoofModule : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        if (pkg == "android" || pkg == "com.astraveil.app" ||
            pkg == "com.astraveil.xposed" || pkg.startsWith("com.android.")
        ) return

        val config = ConfigBridge.load(pkg)
        if (!config.enabled) return

        XposedBridge.log("[AstraSpoof] Hooking $pkg as ${config.profileName}")
        BuildHook.install(config)
        TelephonyHook.install(lpparam.classLoader, config)
        DisplayHook.install(lpparam.classLoader, config)
        SettingsHook.install(lpparam.classLoader, config)
    }
}
