package com.astraveil.app.modules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Built-in module definition.
 *
 * Each built-in module corresponds to an Environment Shield feature
 * toggle. State is persisted to /data/adb/astraveil/shield.json, which
 * the Zygisk module reads at preAppSpecialize.
 */
data class BuiltinModule(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: Category,
    val enabled: Boolean = true,
    /** Whether a reboot is required for the change to take effect (Zygisk-layer). */
    val requiresReboot: Boolean = false,
) {
    enum class Category(val label: String) {
        CORE("Core"),
        SPOOF("Spoof"),
        BYPASS("Bypass"),
    }
}

/**
 * Built-in module registry.
 * Order = UI display order.
 */
object BuiltinModuleRegistry {

    val modules: List<BuiltinModule> = listOf(
        BuiltinModule(
            id = "shield_core",
            name = "Environment Shield",
            description = "Hide Root / Magisk / Xposed / mounts / maps / SELinux / debugger / Frida / Unix socket",
            icon = Icons.Filled.Shield,
            category = BuiltinModule.Category.CORE,
            enabled = true,
        ),
        BuiltinModule(
            id = "spoof_engine",
            name = "Device Spoof Engine",
            description = "Zygisk property hook + GL_RENDERER spoof + /proc/cpuinfo spoof",
            icon = Icons.Filled.PhoneAndroid,
            category = BuiltinModule.Category.SPOOF,
            enabled = true,
        ),
        BuiltinModule(
            id = "bypass_momo",
            name = "MOMO Bypass",
            description = "Hook syscall() to defeat direct-syscall detection + Zygisk .so signature hide",
            icon = Icons.Filled.Search,
            category = BuiltinModule.Category.BYPASS,
            enabled = false,
            requiresReboot = true,
        ),
        BuiltinModule(
            id = "bypass_ruru",
            name = "Ruru Bypass",
            description = "Hook ClassLoader.loadClass + Java method entry protection + Frida port hide",
            icon = Icons.Filled.Search,
            category = BuiltinModule.Category.BYPASS,
            enabled = false,
            requiresReboot = true,
        ),
        BuiltinModule(
            id = "bypass_chunqiu",
            name = "chunqiu Bypass",
            description = "Hide /data/adb directory scan + File.listFiles interception + partition hash",
            icon = Icons.Filled.Search,
            category = BuiltinModule.Category.BYPASS,
            enabled = false,
            requiresReboot = true,
        ),
        BuiltinModule(
            id = "bypass_hunter",
            name = "Hunter Bypass",
            description = "Hide /apex/ su paths + ContentProvider probing + Settings.Global interception",
            icon = Icons.Filled.Search,
            category = BuiltinModule.Category.BYPASS,
            enabled = false,
            requiresReboot = true,
        ),
    )

    fun byId(id: String): BuiltinModule? = modules.find { it.id == id }
}
