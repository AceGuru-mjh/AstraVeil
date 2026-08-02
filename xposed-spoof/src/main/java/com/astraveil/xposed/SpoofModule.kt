package com.astraveil.xposed

import com.astraveil.xposed.hooks.BuildHook
import com.astraveil.xposed.hooks.DisplayHook
import com.astraveil.xposed.hooks.SettingsHook
import com.astraveil.xposed.hooks.TelephonyHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * AstraVeil LSPosed 伪装模块。
 *
 * 与 Zygisk 模块读取同一份 JSON 配置（/data/adb/astraveil/spoof/），
 * 但覆盖 Java 层 API——这是 Zygisk Native hook 无法触及的。
 *
 * 推理：android.os.Build.MODEL 是 static final 字段，
 * 在 Build 类加载时从 SystemProperties 读取并缓存。
 * Zygisk 的 preAppSpecialize 在 Zygote fork 后执行，
 * 但 Build 类可能在 Zygote 中就已加载（预加载类列表）。
 * 因此必须用 XposedHelpers.setStaticObjectField 直接改字段值。
 */
class SpoofModule : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName

        // 排除系统和自身
        if (pkg == "android" ||
            pkg == "com.astraveil.app" ||
            pkg == "com.astraveil.xposed" ||
            pkg.startsWith("com.android.")
        ) return

        // 读取配置（与 Zygisk 共享同一份文件）
        val config = ConfigBridge.load(pkg)
        if (!config.enabled) return

        XposedBridge.log("[AstraSpoof] Hooking $pkg as ${config.profileName}")

        // 安装四层 Java hook
        BuildHook.install(config)
        TelephonyHook.install(lpparam.classLoader, config)
        DisplayHook.install(lpparam.classLoader, config)
        SettingsHook.install(lpparam.classLoader, config)
    }
}
