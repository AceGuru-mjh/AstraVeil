package com.astraveil.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.data.SpoofDatabase
import com.astraveil.app.data.SpoofProfileEntity
import com.astraveil.app.spoof.IntegrityReport
import com.astraveil.app.spoof.SPOOF_PROFILES
import com.astraveil.app.spoof.SpoofAuditLogger
import com.astraveil.app.spoof.SpoofConfigManager
import com.astraveil.app.spoof.SpoofIntegrityChecker
import com.astraveil.app.spoof.SpoofModuleInstaller
import com.astraveil.app.spoof.SpoofOptions
import com.astraveil.app.spoof.SpoofProfile
import com.astraveil.app.spoof.SpoofProfileMapper
import com.astraveil.app.spoof.SpoofPropertyEngine
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v3 Spoof UI state — 配合 [com.astraveil.app.ui.screens.DeviceSpoofScreen]
 * 的 48 档案库 + 完整性校验 + 选项卡 UI。
 *
 * @property currentPlatform  当前设备 ro.board.platform（用于 GPU 风险评估）
 * @property currentProps     当前设备 ro.* 快照（用于 DiffRow 对比）
 * @property activeProfileName 当前已应用档案名（null = 未伪装）
 * @property applying         是否正在应用中（按钮禁用 + spinner）
 * @property options          伪装深度选项
 * @property report           最近一次完整性校验报告（null = 未校验）
 * @property verifying        是否正在校验
 * @property isSpoofed        是否已伪装（控制 reset 按钮可见性）
 * @property notice           成功消息（绿色卡片）
 * @property error            错误消息（红色卡片）
 */
data class SpoofUiState(
    val currentPlatform: String = "",
    val currentProps: Map<String, String> = emptyMap(),
    val activeProfileName: String? = null,
    val applying: Boolean = false,
    val options: SpoofOptions = SpoofOptions(),
    val report: IntegrityReport? = null,
    val verifying: Boolean = false,
    val isSpoofed: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
    // ── v3 三层编排附加状态 ──
    val moduleStatus: SpoofModuleInstaller.ModuleStatus =
        SpoofModuleInstaller.ModuleStatus.UNKNOWN,
    val perAppConfigs: Map<String, String> = emptyMap(),
    val installingModule: Boolean = false,
)

/**
 * v3: 48 档案库 ViewModel — 驱动 [DeviceSpoofScreen]。
 *
 * 与 v2 的差异：
 *   1. 档案库从 [SPOOF_PROFILES] (48 台) 加载，UI 通过 [profilesFlow] 订阅
 *   2. 完整性报告由 [SpoofIntegrityChecker.buildReport] 生成
 *   3. 选项改为 [SpoofOptions]（4 个开关）
 *   4. 重命名 isApplying → applying，activeProfile → activeProfileName
 *      errorMessage → error，successMessage → notice
 */
@Suppress("DEPRECATION")
class DeviceSpoofViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SpoofUiState())
    val uiState: StateFlow<SpoofUiState> = _uiState.asStateFlow()

    private var lastContext: Context? = null

    // ── 档案库 ────────────────────────────────────────────────

    /**
     * 暴露档案库为 Flow<List<SpoofProfileEntity>>。
     *
     * 推理：UI 通过 [SpoofProfileMapper.toDomain] 转 [SpoofProfile]，
     * 这样持久化层（Room）变更不影响 UI。
     *
     * 实现：优先用 [SpoofDatabase]；若 Room 不可用（首次启动或迁移失败），
     * 回退到内存中的 [SPOOF_PROFILES]。
     */
    fun profilesFlow(context: Context): Flow<List<SpoofProfileEntity>> = flow {
        runCatching {
            SpoofDatabase.get(context).profileDao().observeAll()
        }.getOrElse {
            // 回退：从内存预设档案构造 Entity 列表（单次发射）
            kotlinx.coroutines.flow.flowOf(
                SPOOF_PROFILES.map(SpoofProfileMapper::toEntity)
            )
        }.collect { emit(it) }
    }

    /** 暴露品牌去重列表为 Flow<List<String>>。 */
    fun brandsFlow(context: Context): Flow<List<String>> = flow {
        runCatching {
            SpoofDatabase.get(context).profileDao().observeBrands()
        }.getOrElse {
            kotlinx.coroutines.flow.flowOf(
                SPOOF_PROFILES.map { it.brand }.distinct().sorted()
            )
        }.collect { emit(it) }
    }

    // ── 选项 ────────────────────────────────────────────────

    /** 更新伪装深度选项。 */
    fun updateOptions(options: SpoofOptions) {
        _uiState.value = _uiState.value.copy(options = options)
    }

    // ── 当前身份 ────────────────────────────────────────────

    /** 读取当前设备 ro.* 属性快照。无参数版（UI 用 LaunchedEffect 调用）。 */
    fun loadCurrentIdentity() {
        val ctx = lastContext ?: return
        loadCurrentIdentity(ctx)
    }

    /** 读取当前设备 ro.* 属性快照（带 Context）。 */
    fun loadCurrentIdentity(context: Context) {
        lastContext = context
        viewModelScope.launch {
            try {
                val provider = ProviderRegistry.activeProvider()
                if (provider == null || !provider.available()) {
                    _uiState.value = _uiState.value.copy(
                        error = "未检测到 root 后端，无法读取设备属性。",
                    )
                    return@launch
                }
                val keys = listOf(
                    "ro.product.model",
                    "ro.product.brand",
                    "ro.product.manufacturer",
                    "ro.product.device",
                    "ro.product.name",
                    "ro.product.odm.model",
                    "ro.product.system.model",
                    "ro.product.vendor.model",
                    "ro.product.product.model",
                    "ro.product.system_ext.model",
                    "ro.build.fingerprint",
                    "ro.build.display.id",
                    "ro.build.id",
                    "ro.build.version.incremental",
                    "ro.build.version.security_patch",
                    "ro.build.version.release",
                    "ro.build.product",
                    "ro.build.characteristics",
                    "ro.board.platform",
                    "ro.product.board",
                    "ro.hardware",
                    "ro.soc.model",
                    "ro.soc.manufacturer",
                )
                val props = withContext(Dispatchers.IO) {
                    keys.associateWith { k ->
                        provider.execute("getprop $k").stdout.trim()
                    }
                }
                val platform = props["ro.board.platform"] ?: ""
                val fingerprint = props["ro.build.fingerprint"] ?: ""

                // 推理：通过对比 fingerprint 是否在 SPOOF_PROFILES 中
                // 判断当前是否处于已伪装状态。
                val active = SPOOF_PROFILES.firstOrNull { it.fingerprint == fingerprint }

                _uiState.value = _uiState.value.copy(
                    currentPlatform = platform,
                    currentProps = props,
                    activeProfileName = active?.name,
                    isSpoofed = active != null,
                    error = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "读取设备属性失败：${e.message}",
                )
            }
        }
    }

    // ── 应用伪装 ────────────────────────────────────────────

    /**
     * 应用伪装 — 三层同时部署。
     *
     * 层 1+2: 写入 /data/adb/astraveil/spoof/global.json（Zygisk + LSPosed 读取）
     * 层 3:   全局 resetprop（立即生效，无需重启）
     *
     * 应用完成后触发 [verify] 生成完整性报告。
     */
    fun applyProfile(context: Context, profile: SpoofProfile) {
        lastContext = context
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                applying = true, error = null, notice = null,
            )
            try {
                val provider = ProviderRegistry.activeProvider() ?: run {
                    _uiState.value = _uiState.value.copy(
                        applying = false,
                        error = "未检测到 root 后端。",
                    )
                    return@launch
                }

                // 层 1+2：写入配置文件（Zygisk + LSPosed 读取）
                SpoofConfigManager.writeGlobalConfig(
                    context, profile, _uiState.value.options
                ).onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        applying = false,
                        error = "配置写入失败：${e.message}",
                    )
                    return@launch
                }

                // 层 3：全局 resetprop（立即生效）
                val ops = SpoofPropertyEngine.buildOps(profile, _uiState.value.options)
                val setPropCmd = buildSetPropCommand(
                    provider.displayName, _uiState.value.options.persistent,
                )
                withContext(Dispatchers.IO) {
                    ops.forEach { op ->
                        provider.execute("${setPropCmd}${op.key} \"${op.value}\"")
                    }
                }

                // 审计日志
                SpoofAuditLogger.logApply(
                    context = context,
                    profileName = profile.name,
                    providerName = provider.displayName,
                    persistent = _uiState.value.options.persistent,
                )

                _uiState.value = _uiState.value.copy(
                    applying = false,
                    isSpoofed = true,
                    activeProfileName = profile.name,
                    notice = buildString {
                        append("已伪装为 ${profile.name}。")
                        if (_uiState.value.moduleStatus ==
                            SpoofModuleInstaller.ModuleStatus.INSTALLED_ENABLED
                        ) {
                            append(" Zygisk/LSPosed 对新启动的应用生效。")
                        } else {
                            append(" 安装 Zygisk 模块以获得完整覆盖。")
                        }
                    },
                )
                // 重新读取属性 + 校验完整性
                loadCurrentIdentity(context)
                verify(context, profile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    applying = false,
                    error = "伪装失败：${e.message}",
                )
            }
        }
    }

    /** Per-app 伪装（仅作用于指定包名） */
    fun applyPerApp(context: Context, packageName: String, profile: SpoofProfile) {
        viewModelScope.launch {
            SpoofConfigManager.writePerAppConfig(
                context, packageName, profile, _uiState.value.options
            ).onSuccess {
                SpoofConfigManager.forceRestartApp(context, packageName)
                _uiState.value = _uiState.value.copy(
                    perAppConfigs = _uiState.value.perAppConfigs +
                        (packageName to profile.name),
                    notice = "$packageName 已伪装为 ${profile.name}，应用已重启。",
                )
                SpoofAuditLogger.logPerApp(context, packageName, profile.name)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    error = "Per-app 配置失败：${e.message}",
                )
            }
        }
    }

    // ── 恢复 ────────────────────────────────────────────────

    /** 恢复真实身份 — 删除全局配置 + resetprop --delete 关键属性。 */
    fun resetIdentity(context: Context) {
        lastContext = context
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                applying = true, notice = null,
            )
            try {
                val provider = ProviderRegistry.activeProvider() ?: run {
                    _uiState.value = _uiState.value.copy(
                        applying = false,
                        error = "未检测到 root 后端。",
                    )
                    return@launch
                }

                // 清除配置文件
                SpoofConfigManager.clearAll(context)

                // resetprop --delete 关键属性（仅 Magisk 后端支持）
                if (provider.displayName.contains("magisk", ignoreCase = true)) {
                    val keysToDelete = listOf(
                        "ro.product.model",
                        "ro.product.brand",
                        "ro.product.manufacturer",
                        "ro.product.device",
                        "ro.product.name",
                        "ro.build.fingerprint",
                        "ro.build.display.id",
                        "ro.build.id",
                        "ro.board.platform",
                    )
                    val cmd = keysToDelete.joinToString("; ") {
                        "resetprop --delete $it"
                    }
                    withContext(Dispatchers.IO) {
                        provider.execute(cmd)
                    }
                }

                SpoofAuditLogger.logReset(context, provider.displayName)

                _uiState.value = _uiState.value.copy(
                    applying = false,
                    isSpoofed = false,
                    activeProfileName = null,
                    report = null,
                    notice = "已恢复真实身份。重启以完全清理残留属性。",
                )
                loadCurrentIdentity(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    applying = false,
                    error = "恢复失败：${e.message}",
                )
            }
        }
    }

    // ── Zygisk 模块 ────────────────────────────────────────

    /** 检查 Zygisk 模块安装状态（层 1+2 前置条件） */
    fun checkModuleStatus(context: Context) {
        viewModelScope.launch {
            val status = SpoofModuleInstaller.getStatus(context)
            _uiState.value = _uiState.value.copy(moduleStatus = status)
        }
    }

    /** 从 app assets 一键安装 Zygisk 模块到 /data/adb/modules/ */
    fun installModule(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(installingModule = true)
            SpoofModuleInstaller.install(context)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        installingModule = false,
                        moduleStatus = SpoofModuleInstaller.ModuleStatus.INSTALLED_ENABLED,
                        notice = "Zygisk 模块已安装。重启后生效。",
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        installingModule = false,
                        error = "模块安装失败：${e.message}",
                    )
                }
        }
    }

    // ── Private helpers ──

    /** 校验伪装完整性 — 重新读取属性并对比 [profile] 生成报告。 */
    private fun verify(context: Context, profile: SpoofProfile) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(verifying = true)
            try {
                // 等待 resetprop 在系统中生效（异步）
                kotlinx.coroutines.delay(200)
                val provider = ProviderRegistry.activeProvider()
                if (provider == null) {
                    _uiState.value = _uiState.value.copy(verifying = false)
                    return@launch
                }
                // 重新读取属性快照
                val keys = listOf(
                    "ro.product.model", "ro.product.brand", "ro.product.device",
                    "ro.build.fingerprint", "ro.build.id",
                    "ro.build.version.security_patch", "ro.board.platform",
                    "ro.product.odm.model",
                )
                val freshProps = withContext(Dispatchers.IO) {
                    keys.associateWith { k ->
                        provider.execute("getprop $k").stdout.trim()
                    }
                }
                val currentPlatform = freshProps["ro.board.platform"] ?: ""
                val report = SpoofIntegrityChecker.buildReport(
                    profile = profile,
                    currentProps = freshProps,
                    currentPlatform = currentPlatform,
                )
                _uiState.value = _uiState.value.copy(
                    report = report,
                    verifying = false,
                    currentProps = _uiState.value.currentProps + freshProps,
                    currentPlatform = currentPlatform,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(verifying = false)
            }
        }
    }

    /**
     * 抽象层核心：根据 root 后端生成不同的 setprop 命令。
     *
     * Magisk:   resetprop [-p] key value
     * KernelSU: ksud setprop key value  (不确定标注：持久性需实测)
     * APatch:   apd setprop key value   (不确定标注：持久性需实测)
     * 通用:     setprop key value       (非持久化，重启丢失)
     */
    private fun buildSetPropCommand(
        providerName: String,
        persistent: Boolean,
    ): String = when {
        providerName.contains("magisk", ignoreCase = true) ->
            if (persistent) "resetprop -p " else "resetprop "
        providerName.contains("kernelsu", ignoreCase = true) ->
            "ksud setprop "
        providerName.contains("apatch", ignoreCase = true) ->
            "apd setprop "
        else ->
            "setprop "
    }
}
