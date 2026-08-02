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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.AstraGlassTopBar
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.viewmodel.DeviceSpoofViewModel
import com.astraveil.app.viewmodel.PRESET_PROFILES
import com.astraveil.app.viewmodel.SpoofProfile
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun DeviceSpoofScreen(
    onNavigateBack: () -> Unit,
    viewModel: DeviceSpoofViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var persistent by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentIdentity(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AstraGlassTopBar(
            title = "Device Spoof",
            hazeState = hazeState,
            onBack = onNavigateBack,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 当前设备标识 ──
            item {
                Spacer(Modifier.height(8.dp))
                AstraCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Current Device Identity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    IdentityRow("Model", state.currentModel)
                    IdentityRow("Brand", state.currentBrand)
                    IdentityRow("Manufacturer", state.currentManufacturer)
                    IdentityRow("Device", state.currentDevice)
                    IdentityRow("Fingerprint", state.currentFingerprint)

                    if (state.isSpoofed) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = AstraTeal,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Spoofed as: ${state.activeProfile}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AstraTeal,
                            )
                        }
                    }
                }
            }

            // ── 持久化选项 ──
            item {
                AstraCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Persist after reboot",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Uses resetprop -p (Magisk) to survive reboot",
                                style = MaterialTheme.typography.bodySmall,
                                color = AstraOnSurfaceMuted,
                            )
                        }
                        Switch(
                            checked = persistent,
                            onCheckedChange = { persistent = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AstraTeal,
                            ),
                        )
                    }
                }
            }

            // ── 预设配置 ──
            item {
                Text(
                    text = "Preset Profiles",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(PRESET_PROFILES) { profile ->
                SpoofProfileCard(
                    profile = profile,
                    isActive = state.activeProfile == profile.name,
                    isApplying = state.isApplying,
                    onApply = {
                        viewModel.applySpoof(context, profile, persistent)
                    },
                )
            }

            // ── 重置 ──
            item {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { viewModel.resetIdentity(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isSpoofed && !state.isApplying,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Reset to Real Identity")
                }
            }

            // ── 状态消息 ──
            if (state.successMessage != null) {
                item {
                    AstraCard(
                        modifier = Modifier.fillMaxWidth(),
                        accent = AstraSuccess.copy(alpha = 0.08f),
                    ) {
                        Text(
                            text = state.successMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = AstraSuccess,
                        )
                    }
                }
            }
            if (state.errorMessage != null) {
                item {
                    AstraCard(
                        modifier = Modifier.fillMaxWidth(),
                        accent = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                    ) {
                        Text(
                            text = state.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun IdentityRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AstraOnSurfaceMuted,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = value.ifEmpty { "—" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SpoofProfileCard(
    profile: SpoofProfile,
    isActive: Boolean,
    isApplying: Boolean,
    onApply: () -> Unit,
) {
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        accent = if (isActive) AstraTeal.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.PhoneAndroid,
                contentDescription = null,
                tint = if (isActive) AstraTeal else AstraOnSurfaceMuted,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${profile.brand} · ${profile.device}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
            }
            if (isActive) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Active",
                    tint = AstraTeal,
                )
            } else {
                TextButton(
                    onClick = onApply,
                    enabled = !isApplying,
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Apply", color = AstraAccent)
                    }
                }
            }
        }
    }
}
