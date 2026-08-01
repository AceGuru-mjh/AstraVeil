package com.astraveil.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.core.execution.CommandAuditLogger
import com.astraveil.modules.security.DeveloperKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Backup settings — export config / trusted keys / audit log to the app's
 * files directory, plus an import placeholder.
 *
 * The exported files land in `context.filesDir` and the absolute path is
 * shown on the screen so the user can find them via the system file
 * picker or `adb pull`.
 */
@Composable
fun BackupSettingsScreen() {
    val context = LocalContext.current
    val core = AstraVeilApplication.core
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    fun export(what: String, block: suspend () -> File) {
        working = true
        status = null
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { block() }
                status = "Exported $what → ${file.absolutePath}"
            } catch (t: Throwable) {
                status = "Export failed: ${t.message ?: "unknown error"}"
            } finally {
                working = false
            }
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
                    text = "Backup",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Export and import AstraVeil data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = "Each export writes a JSON copy to the app's private files directory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
                Spacer(Modifier.padding(top = 8.dp))
                Button(
                    enabled = !working,
                    onClick = {
                        export("config") {
                            val cfg = core.config.load()
                            val json = Json { prettyPrint = true }
                            val text = json.encodeToString(com.astraveil.core.config.AstraConfig.serializer(), cfg)
                            val file = File(context.filesDir, "astra_config_export.json")
                            file.writeText(text)
                            file
                        }
                    },
                ) { Text("Export config") }
                Spacer(Modifier.padding(top = 6.dp))
                Button(
                    enabled = !working,
                    onClick = {
                        export("trusted keys") {
                            val keys = DeveloperKeyStore(context).all()
                            val json = Json { prettyPrint = true }
                            val text = json.encodeToString(keys)
                            val file = File(context.filesDir, "trusted_keys_export.json")
                            file.writeText(text)
                            file
                        }
                    },
                ) { Text("Export trusted keys") }
                Spacer(Modifier.padding(top = 6.dp))
                Button(
                    enabled = !working,
                    onClick = {
                        export("audit log") {
                            CommandAuditLogger(context).export()
                        }
                    },
                ) { Text("Export audit log") }
                if (working) {
                    Spacer(Modifier.padding(top = 8.dp))
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                status?.let { msg ->
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (msg.startsWith("Exported")) AstraSuccess else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Import",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = "Import coming in Phase 1.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AstraOnSurfaceMuted,
                )
                Text(
                    text = "Restore-from-export needs key conflict resolution and " +
                        "audit-trail merge semantics that are not yet implemented.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedButton(enabled = false, onClick = {}) {
                    Text("Import (disabled)")
                }
            }
        }
    }
}
