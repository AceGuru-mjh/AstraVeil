package com.astraveil.xposed.hooks

import android.os.Build
import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XposedHelpers

object BuildHook {
    fun install(c: SpoofConfig) {
        if (c.model.isNotEmpty()) set("MODEL", c.model)
        if (c.brand.isNotEmpty()) set("BRAND", c.brand)
        if (c.manufacturer.isNotEmpty()) set("MANUFACTURER", c.manufacturer)
        if (c.device.isNotEmpty()) {
            set("DEVICE", c.device)
            set("PRODUCT", c.product.ifEmpty { c.device })
        }
        if (c.fingerprint.isNotEmpty()) set("FINGERPRINT", c.fingerprint)
        if (c.hardware.isNotEmpty()) set("HARDWARE", c.hardware)
        if (c.displayId.isNotEmpty()) set("DISPLAY", c.displayId)
        if (c.buildId.isNotEmpty()) set("ID", c.buildId)

        // ── 新增：构建环境 ──
        c.props["ro.build.host"]?.let { set("HOST", it) }
        c.props["ro.build.user"]?.let { set("USER", it) }
        c.props["ro.bootloader"]?.let { set("BOOTLOADER", it) }
        c.props["ro.hardware"]?.let { set("HARDWARE", it) }
        c.props["ro.product.board"]?.let { set("BOARD", it) }

        // Build.RADIO（基带版本）
        c.props["gsm.version.baseband"]?.let { set("RADIO", it) }

        // Build.TIME（构建时间戳）
        c.props["ro.build.date.utc"]?.let { utc ->
            try {
                XposedHelpers.setStaticLongField(
                    Build::class.java, "TIME", utc.toLong() * 1000L,
                )
            } catch (_: Throwable) {}
        }

        // ── Build.VERSION 子字段 ──
        c.props["ro.build.version.incremental"]?.let {
            setVersion("INCREMENTAL", it)
        }
        c.props["ro.build.version.codename"]?.let {
            setVersion("CODENAME", it)
        }
        c.props["ro.build.version.release"]?.let {
            setVersion("RELEASE", it)
        }

        // Build.SERIAL
        c.props["ro.serialno"]?.let {
            try {
                @Suppress("DEPRECATION")
                XposedHelpers.setStaticObjectField(Build::class.java, "SERIAL", it)
            } catch (_: Throwable) {}
        }

        // ── Build.SUPPORTED_ABIS ──
        c.props["ro.product.cpu.abilist"]?.let { abiList ->
            try {
                val abis = abiList.split(",").toTypedArray()
                XposedHelpers.setStaticObjectField(
                    Build::class.java, "SUPPORTED_ABIS", abis,
                )
                val abis64 = c.props["ro.product.cpu.abilist64"]
                    ?.split(",")?.toTypedArray() ?: arrayOf("arm64-v8a")
                XposedHelpers.setStaticObjectField(
                    Build::class.java, "SUPPORTED_64_BIT_ABIS", abis64,
                )
                val abis32 = c.props["ro.product.cpu.abilist32"]
                    ?.split(",")?.toTypedArray()
                    ?: arrayOf("armeabi-v7a", "armeabi")
                XposedHelpers.setStaticObjectField(
                    Build::class.java, "SUPPORTED_32_BIT_ABIS", abis32,
                )
            } catch (_: Throwable) {}
        }
    }

    private fun set(field: String, value: String) {
        try {
            XposedHelpers.setStaticObjectField(Build::class.java, field, value)
        } catch (_: Throwable) {}
    }

    private fun setVersion(field: String, value: String) {
        try {
            XposedHelpers.setStaticObjectField(
                Build.VERSION::class.java, field, value,
            )
        } catch (_: Throwable) {}
    }
}
