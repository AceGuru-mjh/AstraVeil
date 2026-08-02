package com.astraveil.xposed

import org.json.JSONObject
import java.io.File

/**
 * 与 Zygisk 模块共享的配置格式。
 *
 * 推理：不使用 MMKV 跨进程共享，因为 Zygisk 模块运行在
 * 目标应用进程中（UID = 应用 UID），无法访问管理器应用的
 * MMKV 文件。文件 IPC 是唯一可靠路径：
 *   管理器 (root) 写入 → /data/adb/astraveil/spoof/*.json
 *   Zygisk/LSPosed (任意进程) 读取
 */
data class SpoofConfig(
    val enabled: Boolean = false,
    val profileName: String = "",
    val props: Map<String, String> = emptyMap(),
    val glRenderer: String = "",
    val glVendor: String = "",
    // Java 层专用字段（从 props 中提取）
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
    private const val CONFIG_DIR = "/data/adb/astraveil/spoof"

    fun load(packageName: String): SpoofConfig {
        // per-app 优先
        val perApp = File("$CONFIG_DIR/$packageName.json")
        if (perApp.exists()) return parse(perApp)

        // 全局兜底
        val global = File("$CONFIG_DIR/global.json")
        if (global.exists()) return parse(global)

        return SpoofConfig()
    }

    private fun parse(file: File): SpoofConfig {
        return try {
            val j = JSONObject(file.readText())
            val props = mutableMapOf<String, String>()
            j.optJSONObject("props")?.let { p ->
                p.keys().forEach { key ->
                    props[key] = p.optString(key, "")
                }
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
        } catch (e: Exception) {
            SpoofConfig()
        }
    }
}
