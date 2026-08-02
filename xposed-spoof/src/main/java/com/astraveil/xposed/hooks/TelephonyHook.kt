package com.astraveil.xposed.hooks

import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

/**
 * 拦截 TelephonyManager 的设备标识 API。
 *
 * 推理：部分风控 SDK 通过 getSimOperatorName() 判断设备地区，
 * 通过 getNetworkOperatorName() 交叉验证。如果声称是美版
 * Pixel 但运营商是 "China Mobile"，会产生矛盾。
 */
object TelephonyHook {

    fun install(classLoader: ClassLoader, config: SpoofConfig) {
        val tmClass = "android.telephony.TelephonyManager"

        // 运营商名称
        if (config.simOperator.isNotEmpty()) {
            hookReturn(tmClass, classLoader, "getSimOperatorName",
                config.simOperator)
            hookReturn(tmClass, classLoader, "getNetworkOperatorName",
                config.simOperator)
        }

        // 设备 ID（IMEI/MEID）— 推理：返回空串比返回假 IMEI 更安全，
        // 因为假 IMEI 可能命中黑名单或格式校验失败
        listOf("getDeviceId", "getImei", "getMeid").forEach { method ->
            try {
                XposedHelpers.findAndHookMethod(
                    tmClass, classLoader, method,
                    XC_MethodReplacement.returnConstant("")
                )
            } catch (_: Throwable) {
                // 方法签名可能因 Android 版本不同
            }
        }
    }

    private fun hookReturn(
        className: String, classLoader: ClassLoader,
        method: String, value: String,
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                className, classLoader, method,
                XC_MethodReplacement.returnConstant(value)
            )
        } catch (_: Throwable) {}
    }
}
