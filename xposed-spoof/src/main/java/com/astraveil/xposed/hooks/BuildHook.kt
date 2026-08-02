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

        c.props["ro.build.version.incremental"]?.let {
            try {
                XposedHelpers.setStaticObjectField(
                    Build.VERSION::class.java, "INCREMENTAL", it,
                )
            } catch (_: Throwable) {}
        }
        c.props["ro.serialno"]?.let {
            try {
                @Suppress("DEPRECATION")
                XposedHelpers.setStaticObjectField(
                    Build::class.java, "SERIAL", it,
                )
            } catch (_: Throwable) {}
        }
    }

    private fun set(field: String, value: String) {
        try {
            XposedHelpers.setStaticObjectField(Build::class.java, field, value)
        } catch (_: Throwable) {}
    }
}
