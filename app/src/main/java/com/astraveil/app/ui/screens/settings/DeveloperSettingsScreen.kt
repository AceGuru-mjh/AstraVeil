package com.astraveil.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.astraveil.core.version.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val logLevels = listOf("DEBUG" to "Verbose — include all debug logs",
    "INFO" to "Operational events (default)",
    "WARN" to "Warnings and above only",
    "ERROR" to "Errors only — quietest")

/**
 * Developer settings — version metadata, log level bound to
 * [com.astraveil.core.config.AstraConfig.logLevel], and a config-export
 * button (the full diagnostic report lives on the Diagnostics screen).
 */
@Composable
fun DeveloperSettingsScreen() {
    val context = LocalContext.current
    val core = AstraVeilApplication.core
    val scope = rememberCoroutineScope()

    var currentLevel by remember { mutableStateOf("INFO") }
    var loaded by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var exportPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            val cfg = withContext(Dispatchers.IO) { core.config.load() }
            currentLevel = cfg.logLevel.uppercase()
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
                    text = "Developer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Version, log level, diagnostic export",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 6.dp))
                InfoRow(Icons.Filled.Build, "Build", Version.displayString())
                Spacer(Modifier.padding(top = 6.dp))
                InfoRow(Icons.Filled.Code, "API Level", Version.API.toString())
                Spacer(Modifier.padding(top = 6.dp))
                InfoRow(Icons.Filled.Person, "Developer", Version.DEVELOPER)
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Log Level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = "Applied immediately to the in-process logger; persisted to config.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
                Spacer(Modifier.padding(top = 6.dp))
                logLevels.forEach { (level, hint) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            enabled = loaded,
                            selected = currentLevel == level,
                            onClick = {
                                currentLevel = level
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        core.config.update { it.logLevel = level }
                                    }
                                    // Apply to the live logger so the change takes
                                    // effect without a restart.
                                    runCatching {
                                        val ll = com.astraveil.core.logger.LogLevel.valueOf(level)
                                        core.logger.setMinLevel(ll)
                                    }
                                }
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AstraAccent,
                            ),
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = level,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = AstraOnSurfaceMuted,
                            )
                        }
                    }
                }
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Diagnostic Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = "The Diagnostics screen generates the full system report. " +
                        "Here you can export the raw AstraVeil config snapshot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
                Spacer(Modifier.padding(top = 8.dp))
                Button(
                    enabled = !exporting,
                    onClick = {
                        exporting = true
                        exportPath = null
                        scope.launch {
                            try {
                                val path = withContext(Dispatchers.IO) {
                                    val cfg = core.config.load()
                                    val text = Json { prettyPrint = true }
                                        .encodeToString(com.astraveil.core.config.AstraConfig.serializer(), cfg)
                                    val file = File(context.filesDir, "astra_config_export.json")
                                    file.writeText(text)
                                    file.absolutePath
                                }
                                exportPath = path
                            } catch (t: Throwable) {
                                exportPath = "Export failed: ${t.message ?: "unknown error"}"
                            } finally {
                                exporting = false
                            }
                        }
                    },
                ) {
                    if (exporting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Text("Export Diagnostic Config")
                }
                exportPath?.let { p ->
                    Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        text = p,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (p.startsWith("Export failed")) MaterialTheme.colorScheme.error else AstraAccent,
                    )
                }
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = AstraWarning,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                    Text(
                        text = "These settings affect system behavior.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AstraWarning,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.padding(start = 12.dp))
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
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
