package com.astraveil.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.astraveil.app.ui.AstraStrings
import com.astraveil.app.ui.components.StatusPill
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.viewmodel.StatusViewModel
import com.astraveil.core.capability.SelinuxStatus

/**
 * Detailed device + AstraVeil capability explorer.
 *
 * Renders the [com.astraveil.core.capability.CapabilityInfo] as a grouped
 * list with section headers: Device, Platform, Kernel, SELinux, Root,
 * Mount/Namespace/Hook/OverlayFS. Each row is a label/value pair with an
 * optional status pill.
 */
@Composable
fun CapabilityScreen(viewModel: StatusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cap = state.capability

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { ScreenHeader() }

        item {
            Section(title = "Device", icon = Icons.Filled.Devices) {
                CapabilityRow("Manufacturer", cap.deviceManufacturer.ifBlank { "—" })
                CapabilityRow("Brand", cap.deviceBrand.ifBlank { "—" })
                CapabilityRow("Model", cap.deviceModel.ifBlank { "—" })
                CapabilityRow("Fingerprint", cap.fingerprint.ifBlank { "—" })
            }
        }

        item {
            Section(title = "Platform", icon = Icons.Filled.SystemUpdate) {
                CapabilityRow("Android version", cap.androidVersion.ifBlank { "—" })
                CapabilityRow("API level", cap.apiLevel.toString())
                CapabilityRow("ABI", cap.abi.ifBlank { "—" })
            }
        }

        item {
            Section(title = "Kernel", icon = Icons.Filled.Memory) {
                CapabilityRow("Version", cap.kernelVersion.ifBlank { "—" })
            }
        }

        item {
            Section(title = "SELinux", icon = Icons.Filled.Security) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AstraStrings.capMode,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        text = cap.selinuxStatusLabel.ifBlank { "Unknown" },
                        color = selinuxColor(cap.selinuxStatus),
                        icon = selinuxIcon(cap.selinuxStatus)
                    )
                }
            }
        }

        item {
            Section(title = "Root", icon = Icons.Filled.VerifiedUser) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AstraStrings.capDetected,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        text = if (cap.isRooted) "Yes" else "No",
                        color = if (cap.isRooted) AstraSuccess else AstraError,
                        icon = if (cap.isRooted) Icons.Filled.CheckCircle else Icons.Filled.Close
                    )
                }
            }
        }

        item {
            Section(title = "Mount & Sandboxes", icon = Icons.Filled.Layers) {
                BoolRow("Mount master", cap.mountSupported)
                BoolRow("Namespace", cap.namespaceSupported)
                BoolRow("Hook", cap.hookSupported)
                BoolRow("OverlayFS", cap.overlayFsSupported)
            }
        }

        item {
            Section(title = "Build", icon = Icons.Filled.Build) {
                CapabilityRow("Kernel string", cap.kernelVersion.ifBlank { "—" })
            }
        }

        item { Spacer(Modifier.size(8.dp)) }
    }
}

@Composable
private fun ScreenHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = AstraStrings.capTitle,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = AstraStrings.capSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun Section(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AstraAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                color = AstraAccent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BoolRow(label: String, value: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        StatusPill(
            text = if (value) "Supported" else "Unsupported",
            color = if (value) AstraSuccess else AstraError,
            icon = if (value) Icons.Filled.CheckCircle else Icons.Filled.Close
        )
    }
}

private fun selinuxColor(status: SelinuxStatus): Color = when (status) {
    SelinuxStatus.ENFORCING -> AstraSuccess
    SelinuxStatus.PERMISSIVE -> AstraWarning
    SelinuxStatus.DISABLED -> AstraError
    SelinuxStatus.UNKNOWN -> AstraTeal
}

private fun selinuxIcon(status: SelinuxStatus): ImageVector = when (status) {
    SelinuxStatus.ENFORCING -> Icons.Filled.CheckCircle
    SelinuxStatus.DISABLED -> Icons.Filled.Close
    else -> Icons.Filled.Security
}
