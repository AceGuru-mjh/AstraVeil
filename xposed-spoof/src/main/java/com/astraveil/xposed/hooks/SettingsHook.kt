package com.astraveil.xposed.hooks

import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object SettingsHook {
    fun install(cl: ClassLoader, c: SpoofConfig) {
        if (c.androidId.isEmpty()) return
        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Secure", cl,
                "getString",
                android.content.ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        if (p.args[1] as? String == "android_id") {
                            p.result = c.androidId
                        }
                    }
                },
            )
        } catch (_: Throwable) {}
    }
}
