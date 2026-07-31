package com.astraveil.app.hub

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning

/**
 * AstraHub module repository browser.
 *
 * Discovery-only surface: the hub lists curated modules, downloads the
 * selected `.avm`, and verifies its SHA-256 against the index. The
 * actual TrustGate (signature + capability compatibility) is enforced
 * by the installer after the user confirms the hand-off dialog.
 */
@Composable
fun AstraHubScreen(
    viewModel: AstraHubViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- Header (title + refresh) ----
            item { HubHeader(onRefresh = viewModel::refresh) }

            // ---- Search ----
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::search,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Search modules…",
                            color = AstraOnSurfaceMuted,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = AstraOnSurfaceMuted,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }

            // ---- Loading ----
            if (state.loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                    }
                }
            }

            // ---- Error ----
            state.error?.let { err ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AstraWarning.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = err,
                            color = AstraWarning,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss", color = AstraWarning)
                        }
                    }
                }
            }

            // ---- Modules ----
            items(items = state.filtered, key = { it.id }) { module ->
                HubModuleCard(
                    module = module,
                    downloading = state.downloadingId == module.id,
                    onInstall = { viewModel.install(module) },
                )
            }

            if (!state.loading && state.filtered.isEmpty() && state.error == null) {
                item {
                    Text(
                        text = "No modules match your search.",
                        color = AstraOnSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
        }

        // ---- Snackbar anchored to bottom ----
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        )
    }

    // ---- Hand-off dialog ----
    state.pendingInstallPath?.let { path ->
        AlertDialog(
            onDismissRequest = viewModel::consumePendingInstallPath,
            title = { Text("Hand off to installer?") },
            text = {
                Text(
                    "Module downloaded. Hand off to installer? " +
                        "(TrustGate will run)",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.consumePendingInstallPath()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Handed off to installer (path: $path)",
                            )
                        }
                    },
                ) {
                    Text("Confirm", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::consumePendingInstallPath) {
                    Text("Dismiss")
                }
            },
        )
    }
}

// ---- Composable pieces ----------------------------------------------------

@Composable
private fun HubHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = "AstraHub",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh AstraHub",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HubModuleCard(
    module: HubModule,
    downloading: Boolean,
    onInstall: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---- Title row ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = module.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "by ${module.author.ifBlank { "unknown" }} · v${module.version}",
                        color = AstraOnSurfaceMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                TrustBadge(trustLevel = module.trustLevel)
            }

            // ---- Description ----
            if (module.description.isNotBlank()) {
                Text(
                    text = module.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // ---- Capabilities chips ----
            if (module.requiredCapabilities.isNotEmpty()) {
                CapabilityChipRow(
                    label = "Requires",
                    capabilities = module.requiredCapabilities,
                )
            }
            if (module.optionalCapabilities.isNotEmpty()) {
                CapabilityChipRow(
                    label = "Optional",
                    capabilities = module.optionalCapabilities,
                    dim = true,
                )
            }

            // ---- Footer: size + install ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSizeKb(module.sizeBytes),
                    color = AstraOnSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onInstall,
                    enabled = !downloading,
                ) {
                    if (downloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(if (downloading) "Verifying…" else "Install")
                }
            }
        }
    }
}

@Composable
private fun TrustBadge(trustLevel: String) {
    val (label, color) = when (trustLevel.uppercase()) {
        "OFFICIAL" -> "OFFICIAL" to AstraSuccess
        "TRUSTED_DEVELOPER" -> "TRUSTED" to MaterialTheme.colorScheme.primary
        else -> "UNKNOWN" to AstraWarning
    }
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CapabilityChipRow(
    label: String,
    capabilities: List<String>,
    dim: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = AstraOnSurfaceMuted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            capabilities.forEach { cap ->
                val chipColor = if (dim) {
                    AstraOnSurfaceMuted.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = chipColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = cap,
                        color = chipColor,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun formatSizeKb(sizeBytes: Long): String {
    val kb = sizeBytes / 1024.0
    return if (kb >= 1024.0) {
        val mb = kb / 1024.0
        "%.1f MB".format(mb)
    } else {
        "%.0f KB".format(kb)
    }
}
