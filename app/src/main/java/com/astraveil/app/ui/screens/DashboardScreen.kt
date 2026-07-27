package com.astraveil.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraveil.app.ui.components.StatusCard
import com.astraveil.app.ui.components.StatusPill
import com.astraveil.app.ui.components.QuickActionCard
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.viewmodel.StatusViewModel
import com.astraveil.core.capability.SelinuxStatus

@Composable
fun DashboardScreen(
    viewModel: StatusViewModel,
    onNavigate: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HeaderCard(state) }
            item { SystemStatusCard(state) }
            item { PrivilegeBackendCard(state, onNavigate) }
            item { CapabilitiesCard(state) }
            item { ModulesCard(state, onNavigate) }
            item { SecurityCard(state) }
            item { QuickActionsSection(onNavigate) }
        }

        ExtendedFloatingActionButton(
            onClick = { viewModel.refresh() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(20.dp)
                )
            },
            text = { Text(if (state.scanning) "Scanning…" else "Refresh") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }
}

// --------------------------------------------------------------------- cards

@Composable
private fun HeaderCard(state: StatusViewModel.UiState) {
    StatusCard(
        title = "AstraVeil",
        icon = Icons.Filled.Shield,
        status = if (state.scanning) "Scanning" else "Ready",
        accent = AstraAccent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoTile(
                label = "Core",
                value = state.coreVersion,
                modifier = Modifier.weight(1f)
            )
            InfoTile(
                label = "Daemon",
                value = state.daemonStatus.name.lowercase().replaceFirstChar { it.uppercase() },
                valueColor = daemonColor(state.daemonStatus),
                modifier = Modifier.weight(1f)
            )
            InfoTile(
                label = "Provider",
                value = state.providerName,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SystemStatusCard(state: StatusViewModel.UiState) {
    StatusCard(
        title = "System Status",
        icon = Icons.Filled.Dns,
        status = "OK",
        accent = AstraTeal
    ) {
        DataRow(
            icon = Icons.Filled.SystemUpdate,
            label = "Android",
            value = "${state.capability.androidVersion} (API ${state.capability.apiLevel})"
        )
        DataRow(
            icon = Icons.Filled.Storage,
            label = "Kernel",
            value = state.capability.kernelVersion.ifBlank { "unknown" }
        )
        DataRow(
            icon = Icons.Filled.Security,
            label = "SELinux",
            value = state.capability.selinuxStatusLabel.ifBlank { "Unknown" },
            valueColor = selinuxColor(state.capability.selinuxStatus)
        )
        DataRow(
            icon = Icons.Filled.Fingerprint,
            label = "ABI",
            value = state.capability.abi.ifBlank { "unknown" }
        )
    }
}

@Composable
private fun PrivilegeBackendCard(state: StatusViewModel.UiState, onNavigate: (String) -> Unit) {
    val detected = state.providerName != "None"
    val accent = if (detected) AstraSuccess else AstraWarning
    StatusCard(
        title = "Privilege Backend",
        icon = Icons.Filled.AdminPanelSettings,
        status = if (detected) "Detected" else "None",
        accent = accent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (detected) Icons.Filled.CheckCircle else Icons.Filled.Close,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.providerName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (detected) "Version ${state.providerVersion}" else "No root backend detected",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (detected) {
            QuickActionCard(
                title = "Test Root Capability",
                icon = Icons.Filled.Terminal,
                onClick = { onNavigate("provider") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CapabilitiesCard(state: StatusViewModel.UiState) {
    val cap = state.capability
    val items = listOf(
        CapabilityRow("Mount Master", cap.mountSupported),
        CapabilityRow("Namespace", cap.namespaceSupported),
        CapabilityRow("Hook", cap.hookSupported),
        CapabilityRow("OverlayFS", cap.overlayFsSupported)
    )
    StatusCard(
        title = "Capabilities",
        icon = Icons.Filled.VerifiedUser,
        status = "${items.count { it.supported }}/${items.size}",
        accent = AstraTeal
    ) {
        items.forEach { row ->
            CapabilityLine(row)
        }
    }
}

@Composable
private fun ModulesCard(state: StatusViewModel.UiState, onNavigate: (String) -> Unit) {
    StatusCard(
        title = "Modules",
        icon = Icons.Filled.Apps,
        status = if (state.modulesActive > 0) "Active" else "Empty",
        accent = AstraAccent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Extension,
                contentDescription = null,
                tint = AstraAccent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${state.modulesActive} Active",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Astra Modules (.avm)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        QuickActionCard(
            title = "Install Module",
            icon = Icons.Filled.Download,
            onClick = { onNavigate("modules") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SecurityCard(state: StatusViewModel.UiState) {
    val protectedOk = state.securityProtected
    val accent = if (protectedOk) AstraSuccess else AstraError
    StatusCard(
        title = "Security",
        icon = Icons.Filled.Security,
        status = if (protectedOk) "Protected" else "At Risk",
        accent = accent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (protectedOk) Icons.Filled.CheckCircle else Icons.Filled.Close,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (protectedOk)
                    "AstraVeil runtime protections are active."
                else
                    "Security violation detected — review the Security log.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QuickActionsSection(onNavigate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AstraAccent,
        )
        QuickActionCard(
            title = "Install Module",
            icon = Icons.Filled.Download,
            onClick = { onNavigate("modules") },
            modifier = Modifier.fillMaxWidth(),
        )
        QuickActionCard(
            title = "Root Test",
            icon = Icons.Filled.PlayArrow,
            onClick = { onNavigate("provider") },
            modifier = Modifier.fillMaxWidth(),
        )
        QuickActionCard(
            title = "Security",
            icon = Icons.Filled.Security,
            onClick = { onNavigate("provider") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// --------------------------------------------------------------------- bits

@Composable
private fun InfoTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DataRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class CapabilityRow(val name: String, val supported: Boolean)

@Composable
private fun CapabilityLine(row: CapabilityRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        StatusPill(
            text = if (row.supported) "Supported" else "Unsupported",
            color = if (row.supported) AstraSuccess else AstraError,
            icon = if (row.supported) Icons.Filled.CheckCircle else Icons.Filled.Close
        )
    }
}

// --------------------------------------------------------------------- utils

private fun daemonColor(status: StatusViewModel.DaemonStatus): Color = when (status) {
    StatusViewModel.DaemonStatus.ONLINE -> AstraSuccess
    StatusViewModel.DaemonStatus.CONNECTING -> AstraWarning
    StatusViewModel.DaemonStatus.OFFLINE -> AstraError
}

private fun selinuxColor(status: SelinuxStatus): Color = when (status) {
    SelinuxStatus.ENFORCING -> AstraSuccess
    SelinuxStatus.PERMISSIVE -> AstraWarning
    SelinuxStatus.DISABLED -> AstraError
    SelinuxStatus.UNKNOWN -> AstraWarning
}
