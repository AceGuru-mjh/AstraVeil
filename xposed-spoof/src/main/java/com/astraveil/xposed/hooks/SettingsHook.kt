package com.astraveil.xposed.hooks

import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * 拦截 Settings.Secure.getString() 中的 ANDROID_ID 查询。
 *
 * 推理：ANDROID_ID 存储在 Settings 数据库中，不是系统属性，
 * resetprop 无法修改。Zygisk 的 __system_property_get hook
 * 也覆盖不到。只有 Xposed 能在 Java 层拦截这个调用。
 *
 * 注意：修改 ANDROID_ID 会让应用认为是新设备（重新登录、
 * 风控观察期）。这是预期行为，但用户需要知情。
 */
object SettingsHook {

    fun install(classLoader: ClassLoader, config: SpoofConfig) {
        if (config.androidId.isEmpty()) return

        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Secure", classLoader,
                "getString",
                android.content.ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[1] as? String
                        if (key == "android_id") {
                            param.result = config.androidId
                        }
                    }
                }
            )
        } catch (_: Throwable) {}
    }
}
