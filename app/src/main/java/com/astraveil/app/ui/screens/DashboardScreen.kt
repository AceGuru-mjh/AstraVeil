package com.astraveil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraveil.app.ui.AstraStrings
import com.astraveil.app.ui.components.QuickActionCard
import com.astraveil.app.ui.components.StatusCard
import com.astraveil.app.ui.components.StatusPill
import com.astraveil.app.ui.design.GlassCard
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.viewmodel.StatusViewModel
import com.astraveil.core.capability.SelinuxStatus
import com.astraveil.core.compatibility.CompatibilityLevel

@Composable
fun DashboardScreen(
    viewModel: StatusViewModel,
    onNavigate: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HeaderCard(state) }
            item { SystemStatusCard(state) }
            item { DeviceIntelligenceCard(state) }
            item { CompatibilityAssessmentCard(state) }
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
        title = AstraStrings.dashSystemStatus,
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
private fun DeviceIntelligenceCard(state: StatusViewModel.UiState) {
    val dev = state.deviceProfile
    StatusCard(
        title = AstraStrings.dashDeviceIntelligence,
        icon = Icons.Filled.Devices,
        status = dev.model.ifBlank { "Scanning" },
        accent = AstraAccent
    ) {
        DataRow(
            icon = Icons.Filled.Info,
            label = "Manufacturer / Brand",
            value = "${dev.manufacturer.ifBlank { "unknown" }} / ${dev.brand.ifBlank { "unknown" }}"
        )
        DataRow(
            icon = Icons.Filled.LockOpen,
            label = "Bootloader State",
            value = if (dev.bootUnlocked) "Unlocked (Orange)" else "Locked (Green)",
            valueColor = if (dev.bootUnlocked) AstraSuccess else AstraWarning
        )
        DataRow(
            icon = Icons.Filled.VerifiedUser,
            label = "SELinux Profile",
            value = "${dev.selinuxMode} (v${dev.selinuxPolicyVersion})",
            valueColor = if (dev.selinuxEnforcing) AstraSuccess else AstraWarning
        )
    }
}

@Composable
private fun CompatibilityAssessmentCard(state: StatusViewModel.UiState) {
    val res = state.compatibilityResult
    val accentColor = when (res.level) {
        CompatibilityLevel.EXCELLENT -> AstraSuccess
        CompatibilityLevel.GOOD -> AstraTeal
        CompatibilityLevel.LIMITED -> AstraWarning
        CompatibilityLevel.UNSUPPORTED -> AstraError
    }

    StatusCard(
        title = AstraStrings.dashCompatibilityAssessment,
        icon = Icons.Filled.Equalizer,
        status = "${res.score}/100",
        accent = accentColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AstraStrings.dashEnvLevel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = res.level.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { res.score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        if (res.warnings.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = AstraStrings.dashSystemWarnings,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            res.warnings.forEach { warning ->
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AstraWarning,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivilegeBackendCard(state: StatusViewModel.UiState, onNavigate: (String) -> Unit) {
    val detected = state.providerName != "None"
    val accent = if (detected) AstraSuccess else AstraWarning
    StatusCard(
        title = AstraStrings.dashPrivilegeBackend,
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
                title = AstraStrings.dashTestRootCap,
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
    val dev = state.deviceProfile
    val items = listOf(
        CapabilityRow("Mount Master", cap.mountSupported, if (cap.mountSupported) 95 else 10),
        CapabilityRow("Namespace", cap.namespaceSupported, if (cap.namespaceSupported) 90 else 10),
        CapabilityRow("Hook", cap.hookSupported, if (cap.hookSupported) 80 else 10),
        CapabilityRow("OverlayFS", cap.overlayFsSupported, if (dev.kernelOverlayFs) 95 else 15)
    )
    StatusCard(
        title = AstraStrings.dashCapabilities,
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
        title = AstraStrings.dashModulesTitle,
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
                    text = AstraStrings.activeCount(state.modulesActive),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = AstraStrings.dashAstraModules,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        QuickActionCard(
            title = AstraStrings.dashInstallModule,
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
        title = AstraStrings.dashSecurity,
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
            text = AstraStrings.dashQuickActions,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AstraAccent,
        )
        QuickActionCard(
            title = AstraStrings.dashInstallModule,
            icon = Icons.Filled.Download,
            onClick = { onNavigate("modules") },
            modifier = Modifier.fillMaxWidth(),
        )
        QuickActionCard(
            title = AstraStrings.dashRootTest,
            icon = Icons.Filled.PlayArrow,
            onClick = { onNavigate("provider") },
            modifier = Modifier.fillMaxWidth(),
        )
        QuickActionCard(
            title = AstraStrings.dashSecurity,
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

private data class CapabilityRow(val name: String, val supported: Boolean, val confidence: Int)

@Composable
private fun CapabilityLine(row: CapabilityRow) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                text = if (row.supported) "${row.confidence}%" else "Unsupported",
                color = if (row.supported) AstraSuccess else AstraError,
                icon = if (row.supported) Icons.Filled.CheckCircle else Icons.Filled.Close
            )
        }
        if (row.supported) {
            Spacer(Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { row.confidence / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = AstraSuccess,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
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
