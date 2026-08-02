package com.astraveil.xposed

import org.json.JSONObject
import java.io.File

data class SpoofConfig(
    val enabled: Boolean = false,
    val profileName: String = "",
    val props: Map<String, String> = emptyMap(),
    val glRenderer: String = "",
    val glVendor: String = "",
    val model: String = "",
    val brand: String = "",
    val manufacturer: String = "",
    val device: String = "",
    val product: String = "",
    val hardware: String = "",
    val fingerprint: String = "",
    val displayId: String = "",
    val buildId: String = "",
    val androidId: String = "",
    val simOperator: String = "",
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val density: Float = 0f,
)

object ConfigBridge {
    private const val DIR = "/data/adb/astraveil/spoof"

    fun load(pkg: String): SpoofConfig {
        val perApp = File("$DIR/$pkg.json")
        if (perApp.exists()) return parse(perApp)
        val global = File("$DIR/global.json")
        if (global.exists()) return parse(global)
        return SpoofConfig()
    }

    private fun parse(file: File): SpoofConfig {
        return try {
            val j = JSONObject(file.readText())
            val props = mutableMapOf<String, String>()
            j.optJSONObject("props")?.let { p ->
                p.keys().forEach { props[it] = p.optString(it, "") }
            }
            val gl = j.optJSONObject("gl")
            SpoofConfig(
                enabled = j.optBoolean("enabled", false),
                profileName = j.optString("profile", ""),
                props = props,
                glRenderer = gl?.optString("renderer", "") ?: "",
                glVendor = gl?.optString("vendor", "") ?: "",
                model = props["ro.product.model"] ?: "",
                brand = props["ro.product.brand"] ?: "",
                manufacturer = props["ro.product.manufacturer"] ?: "",
                device = props["ro.product.device"] ?: "",
                product = props["ro.product.name"] ?: "",
                hardware = props["ro.hardware"] ?: "",
                fingerprint = props["ro.build.fingerprint"] ?: "",
                displayId = props["ro.build.display.id"] ?: "",
                buildId = props["ro.build.id"] ?: "",
                androidId = j.optString("android_id", ""),
                simOperator = j.optString("sim_operator", ""),
                screenWidth = j.optInt("screen_width", 0),
                screenHeight = j.optInt("screen_height", 0),
                density = j.optDouble("density", 0.0).toFloat(),
            )
        } catch (_: Exception) {
            SpoofConfig()
        }
    }
}
