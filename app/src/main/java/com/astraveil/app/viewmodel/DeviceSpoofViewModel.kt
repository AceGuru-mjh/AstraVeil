package com.astraveil.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.spoof.SpoofConfigManager
import com.astraveil.app.spoof.SpoofModuleInstaller
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SpoofProfile(
    val name: String,
    val model: String,
    val brand: String,
    val manufacturer: String,
    val device: String,
    val fingerprint: String,
    val displayId: String,
    val platform: String = "",
)

/**
 * v3: Spoofing tunables consumed by [SpoofConfigManager] when
 * materialising a profile into the on-disk JSON config consumed by
 * the Zygisk + LSPosed layers.
 *
 * @property persistent  If `true`, the resulting `global.json` is
 *                       stamped with `resetprop_on_boot:true` so the
 *                       Magisk `service.sh` re-applies the props on
 *                       every boot (survives reboots).
 * @property androidId   If `true`, a fresh random `android_id` is
 *                       generated and written into the config — the
 *                       LSPosed `SettingsHook` will return this value
 *                       to any `Settings.Secure.getString("android_id")`
 *                       query inside a hooked process.
 */
data class SpoofOptions(
    val persistent: Boolean = true,
    val androidId: Boolean = false,
)

/**
 * v3: Builds the canonical list of `ro.*` property operations that
 * realise a [SpoofProfile] on the device. Consumed by
 * [com.astraveil.app.spoof.SpoofConfigManager] when serialising the
 * JSON config shared with the Zygisk + LSPosed layers, and by the
 * legacy `applySpoof` resetprop path.
 *
 * The list intentionally covers the cross-validated property set that
 * anti-cheat / risk SDKs read in concert (`ro.product.*.model` family
 * across all partition variants) — leaving any of these unspoofed
 * produces a detectable contradiction.
 */
object SpoofPropertyEngine {

    data class PropOp(val key: String, val value: String)

    fun buildOps(profile: SpoofProfile, options: SpoofOptions): List<PropOp> = buildList {
        add(PropOp("ro.product.model", profile.model))
        add(PropOp("ro.product.brand", profile.brand))
        add(PropOp("ro.product.manufacturer", profile.manufacturer))
        add(PropOp("ro.product.device", profile.device))
        add(PropOp("ro.product.name", profile.device))
        add(PropOp("ro.build.fingerprint", profile.fingerprint))
        add(PropOp("ro.build.display.id", profile.displayId))
        add(PropOp("ro.build.product", profile.device))
        if (profile.platform.isNotEmpty()) {
            add(PropOp("ro.board.platform", profile.platform))
            add(PropOp("ro.product.board", profile.platform))
            add(PropOp("ro.hardware", profile.platform))
        }
        // 子属性（很多 app 读这些进行交叉验证）
        add(PropOp("ro.product.odm.model", profile.model))
        add(PropOp("ro.product.system.model", profile.model))
        add(PropOp("ro.product.vendor.model", profile.model))
        add(PropOp("ro.product.product.model", profile.model))
        add(PropOp("ro.product.system_ext.model", profile.model))
    }

    /**
     * 生成 16 字符十六进制的 ANDROID_ID。推理：真实 ANDROID_ID
     * 在首次刷机时由系统随机生成，格式为 16 个十六进制字符。
     */
    fun generateAndroidId(): String {
        val chars = "0123456789abcdef"
        return buildString {
            repeat(16) { append(chars.random()) }
        }
    }
}

data class SpoofUiState(
    val currentModel: String = "",
    val currentBrand: String = "",
    val currentManufacturer: String = "",
    val currentDevice: String = "",
    val currentFingerprint: String = "",
    val isSpoofed: Boolean = false,
    val activeProfile: String? = null,
    val isApplying: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // ── v3 三层编排新增字段 ──
    val moduleStatus: SpoofModuleInstaller.ModuleStatus =
        SpoofModuleInstaller.ModuleStatus.UNKNOWN,
    val perAppConfigs: Map<String, String> = emptyMap(),  // pkg → profile name
    val installingModule: Boolean = false,
    val options: SpoofOptions = SpoofOptions(),
)

val PRESET_PROFILES = listOf(
    SpoofProfile(
        name = "Pixel 9 Pro",
        model = "Pixel 9 Pro",
        brand = "google",
        manufacturer = "Google",
        device = "caiman",
        fingerprint = "google/caiman/caiman:15/AP3A.250605.015/12345678:user/release-keys",
        displayId = "AP3A.250605.015",
        platform = "zuma",
    ),
    SpoofProfile(
        name = "Samsung Galaxy S25 Ultra",
        model = "SM-S938B",
        brand = "samsung",
        manufacturer = "samsung",
        device = "e3q",
        fingerprint = "samsung/e3qxxx/e3q:15/AP3A.250605.015/S938BXXU1AXA1:user/release-keys",
        displayId = "TP1A.220624.014.S938BXXU1AXA1",
        platform = "sun",
    ),
    SpoofProfile(
        name = "OnePlus 13",
        model = "CPH2651",
        brand = "OnePlus",
        manufacturer = "OnePlus",
        device = "OP5913L1",
        fingerprint = "OnePlus/CPH2651/OP5913L1:15/AP3A.250605.015/1735028400000:user/release-keys",
        displayId = "CPH2651_15.0.0.200(EX01)",
        platform = "sun",
    ),
    SpoofProfile(
        name = "Xiaomi 15 Pro",
        model = "2501DPN30G",
        brand = "Xiaomi",
        manufacturer = "Xiaomi",
        device = "haotian",
        fingerprint = "Xiaomi/haotian/haotian:15/AP3A.250605.015/V816.0.3.0.VBOCNXM:user/release-keys",
        displayId = "V816.0.3.0.VBOCNXM",
        platform = "sun",
    ),
)

@Suppress("DEPRECATION")
class DeviceSpoofViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SpoofUiState())
    val uiState: StateFlow<SpoofUiState> = _uiState.asStateFlow()

    // 最近一次使用的 Context，供私有 helper（applyProfile/verify）复用
    private var lastContext: Context? = null

    fun loadCurrentIdentity(context: Context) {
        lastContext = context
        viewModelScope.launch {
            try {
                val info = runCatching {
                    ProviderRegistry.detectActive()
                }.getOrNull()

                val provider = info?.let {
                    ProviderRegistry.byId(it.providerName)
                }

                if (provider == null || !provider.available()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "No root backend available. Cannot read device properties.",
                    )
                    return@launch
                }

                val props = withContext(Dispatchers.IO) {
                    mapOf(
                        "model" to provider.execute("getprop ro.product.model").stdout.trim(),
                        "brand" to provider.execute("getprop ro.product.brand").stdout.trim(),
                        "manufacturer" to provider.execute("getprop ro.product.manufacturer").stdout.trim(),
                        "device" to provider.execute("getprop ro.product.device").stdout.trim(),
                        "fingerprint" to provider.execute("getprop ro.build.fingerprint").stdout.trim(),
                    )
                }
                _uiState.value = _uiState.value.copy(
                    currentModel = props["model"] ?: "",
                    currentBrand = props["brand"] ?: "",
                    currentManufacturer = props["manufacturer"] ?: "",
                    currentDevice = props["device"] ?: "",
                    currentFingerprint = props["fingerprint"] ?: "",
                    errorMessage = null,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Root required: ${e.message}",
                )
            }
        }
    }

    fun applySpoof(context: Context, profile: SpoofProfile, persistent: Boolean) {
        lastContext = context
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isApplying = true, errorMessage = null, successMessage = null,
            )
            try {
                val info = runCatching {
                    ProviderRegistry.detectActive()
                }.getOrNull()

                val provider = info?.let {
                    ProviderRegistry.byId(it.providerName)
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        errorMessage = "No root backend detected.",
                    )
                    return@launch
                }

                val setPropCmd = buildSetPropCommand(
                    provider.displayName, persistent,
                )

                val commands = listOf(
                    "${setPropCmd}ro.product.model \"${profile.model}\"",
                    "${setPropCmd}ro.product.brand \"${profile.brand}\"",
                    "${setPropCmd}ro.product.manufacturer \"${profile.manufacturer}\"",
                    "${setPropCmd}ro.product.device \"${profile.device}\"",
                    "${setPropCmd}ro.product.name \"${profile.device}\"",
                    "${setPropCmd}ro.build.fingerprint \"${profile.fingerprint}\"",
                    "${setPropCmd}ro.build.display.id \"${profile.displayId}\"",
                    "${setPropCmd}ro.build.product \"${profile.device}\"",
                    // 子属性（很多 app 读这些）
                    "${setPropCmd}ro.product.odm.model \"${profile.model}\"",
                    "${setPropCmd}ro.product.system.model \"${profile.model}\"",
                    "${setPropCmd}ro.product.vendor.model \"${profile.model}\"",
                )

                withContext(Dispatchers.IO) {
                    commands.forEach { cmd ->
                        provider.execute(cmd)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    isSpoofed = true,
                    activeProfile = profile.name,
                    currentModel = profile.model,
                    currentBrand = profile.brand,
                    currentManufacturer = profile.manufacturer,
                    currentDevice = profile.device,
                    currentFingerprint = profile.fingerprint,
                    successMessage = "Device identity spoofed as ${profile.name}",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    errorMessage = "Spoof failed: ${e.message}",
                )
            }
        }
    }

    fun resetIdentity(context: Context) {
        lastContext = context
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isApplying = true, successMessage = null,
            )
            try {
                val info = runCatching {
                    ProviderRegistry.detectActive()
                }.getOrNull()
                val provider = info?.let {
                    ProviderRegistry.byId(it.providerName)
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        errorMessage = "No root backend detected.",
                    )
                    return@launch
                }

                val resetCmd = when {
                    provider.displayName.contains("Magisk", ignoreCase = true) ->
                        "resetprop --delete ro.product.model; " +
                        "resetprop --delete ro.product.brand; " +
                        "resetprop --delete ro.product.manufacturer; " +
                        "resetprop --delete ro.product.device; " +
                        "resetprop --delete ro.build.fingerprint; " +
                        "resetprop --delete ro.build.display.id"
                    else ->
                        // 非 Magisk 后端：重启恢复（非持久化模式下）
                        "echo 'Reboot to restore original identity'"
                }

                withContext(Dispatchers.IO) {
                    provider.execute(resetCmd)
                }

                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    isSpoofed = false,
                    activeProfile = null,
                    successMessage = "Device identity reset. Reboot to fully restore.",
                )
                loadCurrentIdentity(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    errorMessage = "Reset failed: ${e.message}",
                )
            }
        }
    }

    // ──────────────────────────────────────────────── v3 三层编排 ──

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
                        successMessage = "Zygisk 模块已安装。重启后生效。",
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        installingModule = false,
                        errorMessage = "模块安装失败：${e.message}",
                    )
                }
        }
    }

    /**
     * 应用伪装 — 三层同时部署。
     *
     * 层 1+2: 写入 /data/adb/astraveil/spoof/global.json（Zygisk + LSPosed 读取）
     * 层 3:   全局 resetprop（立即生效，无需重启）
     */
    fun applyProfileFull(context: Context, profile: SpoofProfile) {
        lastContext = context
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isApplying = true, errorMessage = null,
            )

            // 层 1+2：写入配置文件（Zygisk + LSPosed 读取）
            SpoofConfigManager.writeGlobalConfig(
                context, profile, _uiState.value.options
            ).onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    errorMessage = "配置写入失败：${e.message}",
                )
                return@launch
            }

            // 层 3：全局 resetprop（立即生效，无需重启）
            applyProfile(profile)  // 上一版的 resetprop 逻辑

            _uiState.value = _uiState.value.copy(
                isApplying = false,
                isSpoofed = true,
                activeProfile = profile.name,
                successMessage = buildString {
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
            verify()
        }
    }

    /** Per-app 伪装（仅作用于指定包名） */
    fun applyPerApp(context: Context, packageName: String, profile: SpoofProfile) {
        viewModelScope.launch {
            SpoofConfigManager.writePerAppConfig(
                context, packageName, profile, _uiState.value.options
            ).onSuccess {
                // 强制重启目标应用使配置立即生效
                SpoofConfigManager.forceRestartApp(context, packageName)
                _uiState.value = _uiState.value.copy(
                    perAppConfigs = _uiState.value.perAppConfigs +
                        (packageName to profile.name),
                    successMessage = "$packageName 已伪装为 ${profile.name}，应用已重启。",
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Per-app 配置失败：${e.message}",
                )
            }
        }
    }

    // ── Private helpers ──

    /** 上一版的 resetprop 逻辑（v3 入口，复用最近的 Context） */
    private fun applyProfile(profile: SpoofProfile) {
        val ctx = lastContext ?: return
        applySpoof(ctx, profile, _uiState.value.options.persistent)
    }

    /** 验证伪装是否生效：重新读取当前设备属性以反映伪装后的状态 */
    private fun verify() {
        lastContext?.let { loadCurrentIdentity(it) }
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
