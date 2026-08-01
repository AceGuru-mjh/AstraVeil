package com.astraveil.app.ui.screens.update

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.LiquidGlass
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.core.update.UpdateState

@Composable
fun UpdateCenterScreen(
    viewModel: UpdateViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { UpdateHeader(viewModel.currentVersion) }
        item { UpdateStatusCard(state, viewModel) }
        when (state) {
            is UpdateState.Available -> {
                item { ReleaseNotesCard((state as UpdateState.Available).releaseNotes) }
            }
            is UpdateState.Downloading -> {
                item { DownloadProgressCard((state as UpdateState.Downloading).progress) }
            }
            else -> {}
        }
    }
}

@Composable
private fun UpdateHeader(currentVersion: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Updates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Text("Current version: $currentVersion", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun UpdateStatusCard(state: UpdateState, viewModel: UpdateViewModel) {
    // P2-17: live update activity (download / verify / install) → LIQUID,
    // violet accent. Idle / available / latest / error → quiet CONTENT.
    val isLive = state is UpdateState.Downloading ||
        state is UpdateState.Verifying ||
        state is UpdateState.Installing
    AstraCard(
        tier = if (isLive) SurfaceTier.LIQUID else SurfaceTier.CONTENT,
        accent = if (isLive) LiquidGlass.AccentViolet else Color.Transparent,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Astra Update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)

            when (state) {
                is UpdateState.Idle -> {
                    Text("Tap check to look for updates.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { viewModel.checkForUpdate() },
                        colors = ButtonDefaults.buttonColors(containerColor = AstraAccent)) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Text("  Check for Update")
                    }
                }
                is UpdateState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = AstraAccent)
                        Spacer(Modifier.width(12.dp))
                        Text("Checking GitHub…", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is UpdateState.Available -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudDownload, null, tint = AstraSuccess, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Update available", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                                color = AstraSuccess)
                            Text("Version: ${state.version}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = { viewModel.downloadAndInstall() },
                        colors = ButtonDefaults.buttonColors(containerColor = AstraAccent)) {
                        Icon(Icons.Filled.CloudDownload, null, modifier = Modifier.size(18.dp))
                        Text("  Download & Install")
                    }
                }
                is UpdateState.Downloading -> {
                    Text("Downloading…", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                is UpdateState.Verifying -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = AstraTeal, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Verifying SHA-256…", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is UpdateState.Installing -> {
                    Text("Installing…", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                is UpdateState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = AstraSuccess, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Update installed successfully!", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, color = AstraSuccess)
                    }
                }
                is UpdateState.Latest -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = AstraSuccess, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("You're on the latest version.", style = MaterialTheme.typography.bodyMedium,
                            color = AstraSuccess)
                    }
                    Button(onClick = { viewModel.checkForUpdate() },
                        colors = ButtonDefaults.buttonColors(containerColor = AstraAccent)) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Text("  Check Again")
                    }
                }
                is UpdateState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, null, tint = AstraError, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(state.message, style = MaterialTheme.typography.bodySmall, color = AstraError)
                    }
                    Button(onClick = { viewModel.checkForUpdate() },
                        colors = ButtonDefaults.buttonColors(containerColor = AstraAccent)) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Text("  Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseNotesCard(notes: String) {
    AstraCard {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Release Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = AstraAccent)
            if (notes.isBlank()) {
                Text("No release notes available.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(progress: Int) {
    // P2-17: this card is only shown while a download is in flight —
    // the live progress indicator makes it inherently ALIVE → LIQUID.
    AstraCard(
        tier = SurfaceTier.LIQUID,
        accent = LiquidGlass.AccentViolet,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Download", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = AstraTeal)
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = AstraAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text("$progress%", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
