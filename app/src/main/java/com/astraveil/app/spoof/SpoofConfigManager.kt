package com.astraveil.app.spoof

import android.content.Context
import com.astraveil.app.viewmodel.SpoofOptions
import com.astraveil.app.viewmodel.SpoofProfile
import com.astraveil.app.viewmodel.SpoofPropertyEngine
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 管理器 → Zygisk/LSPosed 的配置桥梁。
 *
 * 写入路径：/data/adb/astraveil/spoof/
 *   global.json          ← 全局伪装配置
 *   <package>.json       ← per-app 伪装配置
 *
 * 推理：使用文件 IPC 而非 MMKV/Binder，因为：
 *   1. Zygisk 模块以目标应用 UID 运行，无法访问管理器的 MMKV
 *   2. 文件读取是同步的，无竞态（preAppSpecialize 时读一次）
 *   3. root 写入 + 0644 权限 = 所有进程可读
 */
object SpoofConfigManager {

    private const val CONFIG_DIR = "/data/adb/astraveil/spoof"

    /** 写入全局配置 */
    suspend fun writeGlobalConfig(
        context: Context,
        profile: SpoofProfile,
        options: SpoofOptions,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            val provider = ProviderRegistry.detectActive()?.let {
                ProviderRegistry.byId(it.providerName)
            } ?: error("No root provider")

            val ops = SpoofPropertyEngine.buildOps(profile, options)
            val propsJson = JSONObject()
            ops.forEach { op -> propsJson.put(op.key, op.value) }

            val config = JSONObject().apply {
                put("enabled", true)
                put("profile", profile.name)
                put("props", propsJson)
                put("gl", JSONObject().apply {
                    put("renderer", gpuRendererFor(profile.platform))
                    put("vendor", gpuVendorFor(profile.platform))
                })
                put("resetprop_on_boot", options.persistent)
                if (options.androidId) {
                    put("android_id", SpoofPropertyEngine.generateAndroidId())
                }
            }

            // 通过 root 写入（管理器本身无 /data/adb 写权限）
            val escaped = config.toString().replace("'", "'\\''")
            provider.execute("mkdir -p $CONFIG_DIR")
            provider.execute("echo '$escaped' > $CONFIG_DIR/global.json")
            provider.execute("chmod 0644 $CONFIG_DIR/global.json")
        }
    }

    /** 写入 per-app 配置 */
    suspend fun writePerAppConfig(
        context: Context,
        packageName: String,
        profile: SpoofProfile,
        options: SpoofOptions,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            val provider = ProviderRegistry.detectActive()?.let {
                ProviderRegistry.byId(it.providerName)
            } ?: error("No root provider")

            val ops = SpoofPropertyEngine.buildOps(profile, options)
            val propsJson = JSONObject()
            ops.forEach { op -> propsJson.put(op.key, op.value) }

            val config = JSONObject().apply {
                put("enabled", true)
                put("profile", profile.name)
                put("props", propsJson)
                put("gl", JSONObject().apply {
                    put("renderer", gpuRendererFor(profile.platform))
                    put("vendor", gpuVendorFor(profile.platform))
                })
            }

            val escaped = config.toString().replace("'", "'\\''")
            provider.execute("echo '$escaped' > $CONFIG_DIR/$packageName.json")
            provider.execute("chmod 0644 $CONFIG_DIR/$packageName.json")
        }
    }

    /** 删除 per-app 配置（回退到全局） */
    suspend fun removePerAppConfig(
        context: Context,
        packageName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            val provider = ProviderRegistry.detectActive()?.let {
                ProviderRegistry.byId(it.providerName)
            } ?: error("No root provider")
            provider.execute("rm -f $CONFIG_DIR/$packageName.json")
        }
    }

    /** 清除所有配置 */
    suspend fun clearAll(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching<Unit> {
                val provider = ProviderRegistry.detectActive()?.let {
                    ProviderRegistry.byId(it.providerName)
                } ?: error("No root provider")
                provider.execute("rm -f $CONFIG_DIR/*.json")
            }
        }

    /** 强制重启目标应用使配置生效 */
    suspend fun forceRestartApp(
        context: Context,
        packageName: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching<Unit> {
            val provider = ProviderRegistry.detectActive()?.let {
                ProviderRegistry.byId(it.providerName)
            } ?: error("No root provider")
            provider.execute("am force-stop $packageName")
            // 推理：force-stop 后应用下次启动时 Zygote fork 新进程，
            // preAppSpecialize 读取最新配置 → 免重启生效
        }
    }

    // ── GPU 渲染器字符串映射 ──
    // 推理：GL_RENDERER 格式来自实际 GPU 驱动的 glGetString 返回值，
    // 以下为各平台的公开可观察格式
    private fun gpuRendererFor(platform: String): String = when (platform) {
        "sun" -> "Adreno (TM) 830"
        "pineapple" -> "Adreno (TM) 750"
        "kalama" -> "Adreno (TM) 740"
        "crow" -> "Adreno (TM) 735"
        "zuma" -> "Immortalis-G715"
        "cloudripper" -> "Mali-G710"
        "slider" -> "Mali-G78"
        "mt6991" -> "Immortalis-G925"
        "mt6989" -> "Immortalis-G720"
        "mt6897" -> "Mali-G615"
        "mt6877" -> "Mali-G610"
        "exynos1480" -> "Samsung Xclipse 530"
        else -> ""
    }

    private fun gpuVendorFor(platform: String): String = when (platform) {
        "sun", "pineapple", "kalama", "crow" -> "Qualcomm"
        "zuma", "cloudripper", "slider" -> "ARM"
        "mt6991", "mt6989", "mt6897", "mt6877" -> "ARM"
        "exynos1480" -> "Samsung"
        else -> ""
    }
}
