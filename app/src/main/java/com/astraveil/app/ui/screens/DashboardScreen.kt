package com.astraveil.app.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraveil.app.AstraVeilApplication
import com.astraveil.app.root.RootAccessStatus
import com.astraveil.app.ui.Destinations
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.viewmodel.StatusViewModel
import com.astraveil.core.capability.SelinuxStatus
import com.astraveil.core.ipc.DaemonState

/**
 * Dashboard — the home screen of AstraVeil.
 *
 * Merged: CapabilityScreen + ProviderScreen content.
 * Removed: RootTestCard, Refresh button.
 * Added: Device Spoof entry, expandable capability details.
 */
@Composable
fun DashboardScreen(
    viewModel: StatusViewModel,
    onNavigate: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val daemonState by AstraVeilApplication.daemonManager.client.state
        .collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Device header（无 Refresh 按钮）
        item { DeviceHeaderCard(state) }

        // 2. Quick actions（增加 Spoof 入口）
        item { QuickActionsRow(onNavigate) }

        // 3. Loading indicator
        if (state.scanning) {
            item { LoadingCard() }
        }

        // 4. Root access
        item { RootAccessCard(viewModel) }

        // 5. System status（合并 Provider 检测）
        item { SystemStatusCard(state, daemonState) }

        // 6. Capabilities（合并 CapabilityScreen，可展开）
        item { CapabilitiesCard(state) }
    }
}

// ================================================================
// 1. Device header — 无 Refresh 按钮
// ================================================================

@Composable
private fun DeviceHeaderCard(state: StatusViewModel.UiState) {
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val model = Build.MODEL
    val release = Build.VERSION.RELEASE
    val sdk = Build.VERSION.SDK_INT

    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        AstraAccent.copy(alpha = 0.18f),
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = AstraAccent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$manufacturer $model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = "Android $release · API $sdk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // ── 扫描状态指示器（替代 Refresh 按钮） ──
            if (state.scanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

// ================================================================
// 2. Quick actions — 增加 Device Spoof
// ================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsRow(onNavigate: (String) -> Unit) {
    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AstraAccent,
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionButton(
                label = "Terminal",
                icon = Icons.Filled.Terminal,
                color = AstraTeal,
                onClick = { onNavigate("terminal") },
            )
            QuickActionButton(
                label = "Superuser",
                icon = Icons.Filled.SupervisorAccount,
                color = AstraAccent,
                onClick = { onNavigate(Destinations.Superuser.route) },
            )
            QuickActionButton(
                label = "Modules",
                icon = Icons.Filled.Apps,
                color = AstraSuccess,
                onClick = { onNavigate(Destinations.Modules.route) },
            )
            QuickActionButton(
                label = "Spoof",
                icon = Icons.Filled.PhoneAndroid,
                color = AstraWarning,
                onClick = { onNavigate("device_spoof") },
            )
            QuickActionButton(
                label = "Settings",
                icon = Icons.Filled.Settings,
                color = AstraOnSurfaceMuted,
                onClick = { onNavigate(Destinations.Settings.route) },
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ================================================================
// 3. Loading indicator
// ================================================================

@Composable
private fun LoadingCard() {
    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Scanning device…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ================================================================
// 5. System status — 合并 Provider 检测信息
// ================================================================

@Composable
private fun SystemStatusCard(
    state: StatusViewModel.UiState,
    daemonState: DaemonState,
) {
    val cap = state.capability
    AstraCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(
            icon = Icons.Filled.Dns,
            title = "System Status",
            accent = AstraTeal,
        )
        Spacer(Modifier.height(8.dp))
        InfoRow(
            icon = Icons.Filled.SystemUpdate,
            label = "Android",
            value = "${cap.androidVersion.ifBlank { Build.VERSION.RELEASE }} (API ${cap.apiLevel})",
        )
        InfoRow(
            icon = Icons.Filled.Storage,
            label = "Kernel",
            value = cap.kernelVersion.ifBlank { "unknown" },
        )
        InfoRow(
            icon = Icons.Filled.Fingerprint,
            label = "ABI",
            value = cap.abi.ifBlank { "unknown" },
        )
        InfoRow(
            icon = Icons.Filled.Security,
            label = "SELinux",
            value = cap.selinuxStatusLabel.ifBlank { "Unknown" },
            valueColor = selinuxColor(cap.selinuxStatus),
        )
        // ── Provider 检测（原 ProviderScreen 内容） ──
        InfoRow(
            icon = Icons.Filled.VerifiedUser,
            label = "Root Backend",
            value = state.providerName,
            valueColor = if (state.providerName != "None") AstraSuccess else AstraWarning,
        )
        if (state.providerVersion != "—") {
            InfoRow(
                icon = Icons.Filled.VerifiedUser,
                label = "Backend Version",
                value = state.providerVersion,
            )
        }
        InfoRow(
            icon = Icons.Filled.Dns,
            label = "AstraDaemon",
            value = daemonLabel(daemonState),
            valueColor = daemonColor(daemonState),
        )
    }
}

// ================================================================
// 6. Capabilities — 合并 CapabilityScreen，可展开详情
// ================================================================

@Composable
private fun CapabilitiesCard(state: StatusViewModel.UiState) {
    val cap = state.capability
    var expanded by remember { mutableStateOf(false) }

    // 核心能力（始终显示）
    val coreItems = listOf(
        CapabilityItem("Root Access", cap.rootAvailable),
        CapabilityItem(
            "Root Backend",
            cap.rootProvider.isNotBlank() && cap.rootProvider != "none",
        ),
        CapabilityItem("OverlayFS", cap.overlayFsCapability),
        CapabilityItem("SELinux", cap.selinuxStatus == SelinuxStatus.ENFORCING),
    )

    // 扩展能力（原 CapabilityScreen 的 Mount/Namespace/Hook）
    val extendedItems = listOf(
        CapabilityItem("Mount Master", cap.mountSupported),
        CapabilityItem("Namespace", cap.namespaceSupported),
        CapabilityItem("Hook", cap.hookSupported),
    )

    val enabledCount = coreItems.count { it.enabled }
    val total = coreItems.size

    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(
                icon = Icons.Filled.VerifiedUser,
                title = "Capabilities",
                accent = AstraTeal,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .background(
                        AstraTeal.copy(alpha = 0.18f),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "$enabledCount/$total",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AstraTeal,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        coreItems.forEach { item -> CapabilityLine(item) }

        // ── 展开/收起（原 CapabilityScreen 的 Device 详情） ──
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(if (expanded) "Show Less" else "Show All Capabilities")
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                extendedItems.forEach { CapabilityLine(it) }
                Spacer(Modifier.height(8.dp))
                // 原 CapabilityScreen 的 Device section
                InfoRow(Icons.Filled.Devices, "Manufacturer", cap.deviceManufacturer)
                InfoRow(Icons.Filled.Devices, "Brand", cap.deviceBrand)
                InfoRow(Icons.Filled.Devices, "Model", cap.deviceModel)
                InfoRow(Icons.Filled.Fingerprint, "Fingerprint", cap.fingerprint)
            }
        }
    }
}

private data class CapabilityItem(val name: String, val enabled: Boolean)

@Composable
private fun CapabilityLine(item: CapabilityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (item.enabled) Icons.Filled.CheckCircle else Icons.Filled.Block,
            contentDescription = null,
            tint = if (item.enabled) AstraSuccess else AstraError,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (item.enabled) "Enabled" else "Disabled",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (item.enabled) AstraSuccess else AstraOnSurfaceMuted,
        )
    }
}

// ================================================================
// Root access card — 保留原逻辑
// ================================================================

@Composable
private fun RootAccessCard(viewModel: StatusViewModel) {
    val accessStatus by viewModel.rootAccessStatus.collectAsStateWithLifecycle()
    val requesting by viewModel.requestingAccess.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (accessStatus) {
                    RootAccessStatus.GRANTED -> Icons.Filled.CheckCircle
                    RootAccessStatus.DENIED -> Icons.Filled.Lock
                    RootAccessStatus.NO_BACKEND -> Icons.Filled.Lock
                    RootAccessStatus.ERROR -> Icons.Filled.Lock
                    null -> Icons.Filled.Lock
                },
                contentDescription = null,
                tint = when (accessStatus) {
                    RootAccessStatus.GRANTED -> AstraSuccess
                    RootAccessStatus.DENIED -> AstraWarning
                    RootAccessStatus.NO_BACKEND -> MaterialTheme.colorScheme.primary
                    RootAccessStatus.ERROR -> AstraError
                    null -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Root Access",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val statusLabel = when (accessStatus) {
                    RootAccessStatus.GRANTED -> "Granted"
                    RootAccessStatus.DENIED -> "Denied"
                    RootAccessStatus.NO_BACKEND -> "No backend"
                    RootAccessStatus.ERROR -> "Error"
                    null -> "Idle"
                }
                Text(
                    text = "$statusLabel via ${state.providerName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            requesting -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "A system dialog from your root backend just appeared — tap ALLOW / GRANT on it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            accessStatus == RootAccessStatus.GRANTED -> {
                Text(
                    text = "Root granted. AstraVeil can now manage su policies, modules, and run privileged commands.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraSuccess,
                )
            }
            accessStatus == RootAccessStatus.DENIED -> {
                Text(
                    text = "Root request denied. Retry, or grant AstraVeil in your backend's superuser settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraWarning,
                )
            }
            accessStatus == RootAccessStatus.NO_BACKEND -> {
                Text(
                    text = "No root backend detected. Install Magisk, KernelSU, or APatch first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text(
                    text = "AstraVeil needs root to manage your device. Tap below to request it from your root backend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.Button(onClick = { viewModel.requestRootAccess() }) {
                    Icon(Icons.Filled.Lock, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Grant Root Access")
                }
            }
        }
    }
}

// ================================================================
// Reusable bits
// ================================================================

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accent.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
        )
    }
}

private fun daemonLabel(state: DaemonState): String = when (state) {
    DaemonState.ONLINE -> "Online"
    DaemonState.CONNECTING -> "Connecting…"
    DaemonState.OFFLINE -> "Offline"
    DaemonState.FAILED -> "Failed"
}

private fun daemonColor(state: DaemonState): Color = when (state) {
    DaemonState.ONLINE -> AstraSuccess
    DaemonState.CONNECTING -> AstraWarning
    DaemonState.OFFLINE -> AstraError
    DaemonState.FAILED -> AstraError
}

private fun selinuxColor(status: SelinuxStatus): Color = when (status) {
    SelinuxStatus.ENFORCING -> AstraSuccess
    SelinuxStatus.PERMISSIVE -> AstraWarning
    SelinuxStatus.DISABLED -> AstraError
    SelinuxStatus.UNKNOWN -> AstraWarning
}
