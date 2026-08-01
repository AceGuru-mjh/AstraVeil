package com.astraveil.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.AstraVeilApplication
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Daemon settings — AstraDaemon status + enable toggle.
 *
 * The IPC layer (DaemonManager + AstraDaemonClient) exists but is NOT
 * wired into a UI status surface yet (Phase 1 work). This screen is
 * honest about that: it shows the persisted `daemonEnabled` flag and a
 * "Phase 1" status note rather than fabricating an online/offline state.
 */
@Composable
fun DaemonSettingsScreen() {
    val core = AstraVeilApplication.core
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            val cfg = withContext(Dispatchers.IO) { core.config.load() }
            enabled = cfg.daemonEnabled
            loaded = true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Daemon",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "AstraDaemon service status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = "Daemon status: Phase 1",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AstraWarning,
                )
                Text(
                    text = "The AstraDaemon IPC bridge is under active development. " +
                        "It is not yet wired into a runtime status surface — " +
                        "live Online/Offline detection lands in Phase 1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Enable AstraDaemon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start AstraDaemon on launch",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "When enabled, the app will attempt to connect to astrad at boot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AstraOnSurfaceMuted,
                        )
                    }
                    Switch(
                        enabled = loaded,
                        checked = enabled,
                        onCheckedChange = { value ->
                            enabled = value
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    core.config.update { it.daemonEnabled = value }
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AstraAccent,
                            checkedTrackColor = AstraAccent.copy(alpha = 0.35f),
                        ),
                    )
                }
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "About AstraDaemon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = "AstraDaemon is the privileged companion process that " +
                        "executes sandboxed module commands, manages mounts, and " +
                        "enforces SELinux policy. It is started by the Magisk " +
                        "service.sh script when the device boots.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
            }
        }
    }
}
