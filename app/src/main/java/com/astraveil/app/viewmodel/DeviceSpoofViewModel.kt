package com.astraveil.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

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
    ),
    SpoofProfile(
        name = "Samsung Galaxy S25 Ultra",
        model = "SM-S938B",
        brand = "samsung",
        manufacturer = "samsung",
        device = "e3q",
        fingerprint = "samsung/e3qxxx/e3q:15/AP3A.250605.015/S938BXXU1AXA1:user/release-keys",
        displayId = "TP1A.220624.014.S938BXXU1AXA1",
    ),
    SpoofProfile(
        name = "OnePlus 13",
        model = "CPH2651",
        brand = "OnePlus",
        manufacturer = "OnePlus",
        device = "OP5913L1",
        fingerprint = "OnePlus/CPH2651/OP5913L1:15/AP3A.250605.015/1735028400000:user/release-keys",
        displayId = "CPH2651_15.0.0.200(EX01)",
    ),
    SpoofProfile(
        name = "Xiaomi 15 Pro",
        model = "2501DPN30G",
        brand = "Xiaomi",
        manufacturer = "Xiaomi",
        device = "haotian",
        fingerprint = "Xiaomi/haotian/haotian:15/AP3A.250605.015/V816.0.3.0.VBOCNXM:user/release-keys",
        displayId = "V816.0.3.0.VBOCNXM",
    ),
)

@Suppress("DEPRECATION")
class DeviceSpoofViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SpoofUiState())
    val uiState: StateFlow<SpoofUiState> = _uiState.asStateFlow()

    fun loadCurrentIdentity(context: Context) {
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
