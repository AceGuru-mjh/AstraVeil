package com.astraveil.app.spoof

import android.content.Context

/**
 * 自愈任务 — 周期性校验已应用伪装是否仍然有效。
 *
 * 推理：
 *   - 某些系统更新或 Magisk 模块可能 resetprop --delete 已应用的属性
 *   - reboot 后非持久化伪装丢失
 *   - 自愈任务重新读取 /data/adb/astraveil/spoof/global.json
 *     若发现 enabled=true 但实际 ro.build.fingerprint 与配置不符，则记录日志
 *
 * 注册：在 [com.astraveil.app.AstraVeilApplication] 中通过协程或
 *   WorkManager 周期性调用 [execute]。当前实现为协程友好的纯函数式
 *   入口，避免硬依赖 WorkManager（build.gradle 未启用时仍可调用）。
 *
 * 升级路径：当 build.gradle 启用 androidx.work:work-runtime-ktx 后，
 *   可将本类改为 `class SpoofConsistencyHealer(
 *     appContext: Context, params: WorkerParameters
 *   ) : CoroutineWorker(appContext, params)` 并重写 `doWork()` 调用
 *   [execute]。
 */
object SpoofConsistencyHealer {

    /**
     * 执行一次自愈检查。可在任意协程中调用。
     *
     * @return true 若检测到不一致并记录了日志；false 若配置不存在或一致。
     */
    suspend fun execute(context: Context): Boolean {
        return runCatching {
            val provider = com.astraveil.providers.ProviderRegistry.activeProvider()
                ?: return false

            val configExists = provider.execute(
                "[ -f /data/adb/astraveil/spoof/global.json ] && echo yes || echo no"
            ).stdout.trim()
            if (configExists != "yes") return false

            val fingerprint = provider.execute("getprop ro.build.fingerprint")
                .stdout.trim()
            val configuredFp = provider.execute(
                "cat /data/adb/astraveil/spoof/global.json"
            ).stdout.trim()

            // 简单一致性检查：若配置中包含 fingerprint 字段且与当前不一致，
            // 触发日志记录（实际重应用应由用户在 UI 触发）
            if (configuredFp.contains("\"ro.build.fingerprint\"") &&
                !configuredFp.contains(fingerprint)
            ) {
                SpoofAuditLogger.logApply(
                    context = context,
                    profileName = "self-heal",
                    providerName = provider.displayName,
                    persistent = true,
                )
                true
            } else {
                false
            }
        }.getOrElse { false }
    }
}
