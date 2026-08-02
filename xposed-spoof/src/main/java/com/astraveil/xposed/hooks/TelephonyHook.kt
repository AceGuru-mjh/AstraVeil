package com.astraveil.xposed.hooks

import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

object TelephonyHook {
    fun install(cl: ClassLoader, c: SpoofConfig) {
        val tm = "android.telephony.TelephonyManager"
        if (c.simOperator.isNotEmpty()) {
            ret(tm, cl, "getSimOperatorName", c.simOperator)
            ret(tm, cl, "getNetworkOperatorName", c.simOperator)
        }
        listOf("getDeviceId", "getImei", "getMeid").forEach { m ->
            try {
                XposedHelpers.findAndHookMethod(
                    tm, cl, m, XC_MethodReplacement.returnConstant(""),
                )
            } catch (_: Throwable) {}
        }
    }

    private fun ret(cls: String, cl: ClassLoader, m: String, v: String) {
        try {
            XposedHelpers.findAndHookMethod(
                cls, cl, m, XC_MethodReplacement.returnConstant(v),
            )
        } catch (_: Throwable) {}
    }
}
