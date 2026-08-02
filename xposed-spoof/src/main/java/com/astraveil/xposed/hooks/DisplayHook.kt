package com.astraveil.xposed.hooks

import android.util.DisplayMetrics
import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

object DisplayHook {
    fun install(cl: ClassLoader, c: SpoofConfig) {
        if (c.screenWidth == 0 || c.screenHeight == 0) return
        try {
            XposedHelpers.findAndHookMethod(
                "android.view.Display", cl,
                "getMetrics", DisplayMetrics::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        val dm = p.args[0] as? DisplayMetrics ?: return
                        dm.widthPixels = c.screenWidth
                        dm.heightPixels = c.screenHeight
                        if (c.density > 0f) {
                            dm.density = c.density
                            dm.densityDpi = (c.density * 160).toInt()
                            dm.scaledDensity = c.density
                            dm.xdpi = c.density * 160
                            dm.ydpi = c.density * 160
                        }
                    }
                },
            )
        } catch (_: Throwable) {}

        try {
            XposedHelpers.findAndHookMethod(
                "android.content.res.Resources", cl,
                "getDisplayMetrics",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        val dm = p.result as? DisplayMetrics ?: return
                        dm.widthPixels = c.screenWidth
                        dm.heightPixels = c.screenHeight
                        if (c.density > 0f) {
                            dm.density = c.density
                            dm.densityDpi = (c.density * 160).toInt()
                        }
                    }
                },
            )
        } catch (_: Throwable) {}
    }
}
