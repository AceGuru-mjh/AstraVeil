package com.astraveil.xposed.hooks

import android.os.Build
import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XposedHelpers

/**
 * 覆写 android.os.Build 的所有静态字段。
 *
 * 推理：Build.MODEL 等是 static final String，JVM 在类加载时
 * 从 SystemProperties 读取。即使 Zygisk 已 hook 了
 * __system_property_get，Build 类可能在 Zygote 预加载阶段
 * 就已初始化（android.os.Build 在 Zygote 的预加载类列表中）。
 * 因此必须用反射直接改字段值。
 *
 * 覆盖字段清单（基于 AOSP android.os.Build 源码）：
 *   BOARD, BRAND, DEVICE, FINGERPRINT, HARDWARE, HOST, ID,
 *   MANUFACTURER, MODEL, PRODUCT, TAGS, TYPE, USER,
 *   DISPLAY, INCREMENTAL, RELEASE, SDK_INT (危险，默认跳过)
 */
object BuildHook {

    fun install(config: SpoofConfig) {
        if (config.model.isNotEmpty()) {
            setField("MODEL", config.model)
        }
        if (config.brand.isNotEmpty()) {
            setField("BRAND", config.brand)
        }
        if (config.manufacturer.isNotEmpty()) {
            setField("MANUFACTURER", config.manufacturer)
        }
        if (config.device.isNotEmpty()) {
            setField("DEVICE", config.device)
            setField("PRODUCT", config.product.ifEmpty { config.device })
        }
        if (config.fingerprint.isNotEmpty()) {
            setField("FINGERPRINT", config.fingerprint)
        }
        if (config.hardware.isNotEmpty()) {
            setField("HARDWARE", config.hardware)
        }
        if (config.displayId.isNotEmpty()) {
            setField("DISPLAY", config.displayId)
        }
        if (config.buildId.isNotEmpty()) {
            setField("ID", config.buildId)
        }

        // ── Build.VERSION 子字段 ──
        // 推理：RELEASE 和 SDK_INT 只在目标版本 == 当前版本时修改
        // 否则应用调用不存在的 API → NoSuchMethodError
        // 此处仅修改 INCREMENTAL（安全）
        val incremental = config.props["ro.build.version.incremental"]
        if (!incremental.isNullOrEmpty()) {
            try {
                XposedHelpers.setStaticObjectField(
                    Build.VERSION::class.java, "INCREMENTAL", incremental
                )
            } catch (_: Throwable) {}
        }

        // ── Build.SERIAL（已废弃但部分应用仍读取） ──
        val serial = config.props["ro.serialno"]
        if (!serial.isNullOrEmpty()) {
            try {
                @Suppress("DEPRECATION")
                XposedHelpers.setStaticObjectField(
                    Build::class.java, "SERIAL", serial
                )
            } catch (_: Throwable) {}
        }
    }

    private fun setField(field: String, value: String) {
        try {
            XposedHelpers.setStaticObjectField(Build::class.java, field, value)
        } catch (e: Throwable) {
            // 字段不存在（不同 Android 版本）→ 静默跳过
        }
    }
}
