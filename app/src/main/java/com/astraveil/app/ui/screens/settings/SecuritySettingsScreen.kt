package com.astraveil.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.AstraVeilApplication
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.core.execution.CommandAuditLogger
import com.astraveil.modules.security.DeveloperKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Security settings — permission mode, trusted developer keys, audit log.
 *
 * "Strict mode" = NOT `dangerousApproval`. When strict is on, the policy
 * gate refuses dangerous permission grants; when off, dangerous grants are
 * allowed (after the usual user prompt). Bound to [AstraConfig.dangerousApproval]
 * via `ConfigManager.update`.
 */
@Composable
fun SecuritySettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val core = AstraVeilApplication.core

    var strictMode by remember { mutableStateOf(false) }
    var trustedKeys by remember { mutableStateOf<List<com.astraveil.modules.security.TrustedKey>>(emptyList()) }
    var auditEntries by remember { mutableStateOf<List<com.astraveil.core.execution.CommandAuditEntry>>(emptyList()) }
    var showAuditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            val cfg = withContext(Dispatchers.IO) { core.config.load() }
            strictMode = !cfg.dangerousApproval
            trustedKeys = withContext(Dispatchers.IO) { DeveloperKeyStore(context).all() }
            auditEntries = withContext(Dispatchers.IO) { CommandAuditLogger(context).recent(50) }
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
                    text = "Security",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Permission mode, trusted keys, audit log",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Permission Mode",
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
                            text = "Strict mode",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Refuse dangerous permission grants entirely. Off = allow after prompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AstraOnSurfaceMuted,
                        )
                    }
                    Switch(
                        checked = strictMode,
                        onCheckedChange = { value ->
                            strictMode = value
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    core.config.update { it.dangerousApproval = !value }
                                    core.permissionEngine.setDangerousApproval(!value)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AstraWarning,
                            checkedTrackColor = AstraWarning.copy(alpha = 0.35f),
                        ),
                    )
                }
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Trusted Developer Keys",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                if (trustedKeys.isEmpty()) {
                    Text(
                        text = "No trusted keys",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstraOnSurfaceMuted,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        trustedKeys.forEach { key ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    text = key.label.ifBlank { "Unnamed key" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = key.fingerprint,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = AstraOnSurfaceMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Audit Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = "${auditEntries.size} recent command(s) recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedButton(onClick = { showAuditDialog = true }) {
                    Text("View Audit Log")
                }
            }
        }
    }

    if (showAuditDialog) {
        AuditLogDialog(
            entries = auditEntries,
            onDismiss = { showAuditDialog = false },
        )
    }
}

@Composable
private fun AuditLogDialog(
    entries: List<com.astraveil.core.execution.CommandAuditEntry>,
    onDismiss: () -> Unit,
) {
    val df = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.US) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Audit Log (${entries.size})") },
        text = {
            if (entries.isEmpty()) {
                Text(
                    text = "No audit entries yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    entries.forEach { e ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${df.format(Date(e.timestamp))}  ·  ${e.source}  ·  ${e.backend}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AstraOnSurfaceMuted,
                            )
                            Text(
                                text = "$ ${e.command}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = if (e.success) MaterialTheme.colorScheme.onSurface else AstraWarning,
                            )
                        }
                    }
                }
            }
        },
    )
}
