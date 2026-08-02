package com.astraveil.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.spoof.EnvShieldManager
import com.astraveil.app.spoof.EnvShieldManager.ShieldState
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.AstraGlassTopBar
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

@Composable
fun EnvShieldScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }
    var state by remember { mutableStateOf(ShieldState()) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        state = EnvShieldManager.readConfig(context)
    }

    fun save(newState: ShieldState) {
        state = newState
        saving = true
        scope.launch {
            EnvShieldManager.writeConfig(context, newState)
            saving = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AstraGlassTopBar(
            title = "Environment Shield",
            hazeState = hazeState,
            onBack = onNavigateBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // -- Master switch --
            AstraCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Shield, null,
                        tint = if (state.enabled) AstraSuccess else AstraOnSurfaceMuted,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Environment Shield",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (state.enabled) "Active — hiding all modification traces from target apps"
                            else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = AstraOnSurfaceMuted,
                        )
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = { save(state.copy(enabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = AstraSuccess,
                        ),
                    )
                }
            }

            // -- General protection --
            Text(
                "General Protection",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AstraAccent,
            )
            AstraCard(modifier = Modifier.fillMaxWidth()) {
                ShieldRow("Hide Root / su",
                    "su binary, Superuser apps, root management packages",
                    state.hideRoot) { save(state.copy(hideRoot = it)) }
                ShieldRow("Hide Magisk / Zygisk",
                    "/data/adb, MagiskManager packages, Zygisk .so",
                    state.hideMagisk) { save(state.copy(hideMagisk = it)) }
                ShieldRow("Hide Xposed / LSPosed",
                    "Class.forName probing, package scanning, framework files",
                    state.hideXposed) { save(state.copy(hideXposed = it)) }
                ShieldRow("Hide mount traces",
                    "Magisk overlay in /proc/self/mountinfo",
                    state.hideMounts) { save(state.copy(hideMounts = it)) }
                ShieldRow("Hide /proc/self/maps",
                    "Filter module .so, dobby, zygisk loaded lines",
                    state.hideMaps) { save(state.copy(hideMaps = it)) }
                ShieldRow("Spoof SELinux = Enforcing",
                    "/sys/fs/selinux/enforce returns 1",
                    state.hideSelinux) { save(state.copy(hideSelinux = it)) }
                ShieldRow("Hide debugger (TracerPid=0)",
                    "Set TracerPid to 0 in /proc/self/status",
                    state.hideDebugger) { save(state.copy(hideDebugger = it)) }
                ShieldRow("Hide Frida",
                    "Filter port 27042/27043 in /proc/net/tcp",
                    state.hideFrida) { save(state.copy(hideFrida = it)) }
                ShieldRow("Hide Unix Socket",
                    "Filter magisk/zygisk socket in /proc/net/unix",
                    state.hideNetUnix) { save(state.copy(hideNetUnix = it)) }
            }

            // -- Per-tool bypass --
            Text(
                "Detection Tool Bypass",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AstraWarning,
            )
            AstraCard(modifier = Modifier.fillMaxWidth()) {
                ShieldRow("MOMO Bypass",
                    "Extra syscall() hook to defeat direct-syscall detection",
                    state.momoBypass) { save(state.copy(momoBypass = it)) }
                ShieldRow("Ruru Bypass",
                    "ClassLoader.loadClass + method entry protection + Frida port",
                    state.ruruBypass) { save(state.copy(ruruBypass = it)) }
                ShieldRow("chunqiu Bypass",
                    "/data/adb directory scan + File.listFiles interception",
                    state.chunqiuBypass) { save(state.copy(chunqiuBypass = it)) }
                ShieldRow("Hunter Bypass",
                    "/apex/ su paths + ContentProvider + Settings.Global",
                    state.hunterBypass) { save(state.copy(hunterBypass = it)) }
            }

            // -- How it works --
            AstraCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "How it works",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The Environment Shield intercepts openat/syscall at the " +
                    "native layer via the Zygisk module, and intercepts " +
                    "Class.forName/PackageManager/File at the Java layer via " +
                    "the LSPosed module. Both layers work together to cover " +
                    "all known detection vectors.\n\n" +
                    "Config takes effect for newly launched app processes. " +
                    "Already-running apps must be force-stopped and reopened.\n\n" +
                    "Warning: cannot bypass Play Integrity hardware attestation " +
                    "(TEE-signed).",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShieldRow(
    title: String,
    desc: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = AstraOnSurfaceMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AstraTeal,
            ),
        )
    }
}
