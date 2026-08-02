package com.astraveil.xposed.hooks

import android.util.DisplayMetrics
import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * 伪造屏幕分辨率和密度。
 *
 * 推理：部分应用通过 DisplayMetrics 交叉验证机型。
 * 例如 Pixel 9 Pro 的分辨率是 1344x2992，密度 480dpi。
 * 如果声称是 Pixel 9 Pro 但分辨率是 1080x2400，会被检测。
 *
 * 限制：实际渲染仍使用真实分辨率（SurfaceFlinger 层），
 * 只有 API 查询返回伪造值。这足以通过大多数应用层检测。
 */
object DisplayHook {

    fun install(classLoader: ClassLoader, config: SpoofConfig) {
        if (config.screenWidth == 0 || config.screenHeight == 0) return

        // Hook DisplayMetrics 的 setTo / setToDefaults
        try {
            XposedHelpers.findAndHookMethod(
                "android.view.Display", classLoader,
                "getMetrics", DisplayMetrics::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val dm = param.args[0] as? DisplayMetrics ?: return
                        dm.widthPixels = config.screenWidth
                        dm.heightPixels = config.screenHeight
                        if (config.density > 0f) {
                            dm.density = config.density
                            dm.densityDpi = (config.density * 160).toInt()
                            dm.scaledDensity = config.density
                            dm.xdpi = config.density * 160
                            dm.ydpi = config.density * 160
                        }
                    }
                }
            )
        } catch (_: Throwable) {}

        // Hook Resources.getDisplayMetrics
        try {
            XposedHelpers.findAndHookMethod(
                "android.content.res.Resources", classLoader,
                "getDisplayMetrics",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val dm = param.result as? DisplayMetrics ?: return
                        dm.widthPixels = config.screenWidth
                        dm.heightPixels = config.screenHeight
                        if (config.density > 0f) {
                            dm.density = config.density
                            dm.densityDpi = (config.density * 160).toInt()
                        }
                    }
                }
            )
        } catch (_: Throwable) {}
    }
}
