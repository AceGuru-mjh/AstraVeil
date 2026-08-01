package com.astraveil.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.ui.screens.update.UpdateViewModel
import com.astraveil.core.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Update & Backup — merged page (P2-16 consolidation).
 *
 * Update: check / download / verify / install via [UpdateViewModel].
 *   The ViewModel exposes a sealed [UpdateState] that this screen
 *   `when`-matches to render the right card (checking spinner, available
 *   card, progress bar, error message, etc.).
 *
 * Backup: export/import a ZIP containing the module registry,
 * trusted developer keys, privileged command audit log, and a backup
 * meta marker. Uses SAF (CreateDocument / OpenDocument) so no storage
 * permission is needed.
 */
@Composable
fun UpdateBackupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateVm: UpdateViewModel = viewModel()
    val updateState by updateVm.state.collectAsStateWithLifecycle()
    val currentVersion = updateVm.currentVersion

    var backupStatus by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.let {
            scope.launch {
                backupStatus = "Exporting…"
                val ok = withContext(Dispatchers.IO) { exportBackup(context, it) }
                backupStatus = if (ok) "Backup exported." else "Export failed."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            scope.launch {
                backupStatus = "Restoring…"
                val ok = withContext(Dispatchers.IO) { importBackup(context, it) }
                backupStatus = if (ok) "Restored. Restart the app to apply."
                         else "Restore failed (invalid backup?)."
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Update & Backup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
        }

        // ══════════════ 更新 ══════════════
        item {
            AstraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SystemUpdate, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("App Update",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))

                Text("Current: v$currentVersion",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted)
                Spacer(Modifier.height(10.dp))

                when (val s = updateState) {
                    is UpdateState.Idle, is UpdateState.Latest, is UpdateState.Available, is UpdateState.Error -> {
                        if (s is UpdateState.Latest) {
                            Text("You're on the latest version.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AstraSuccess)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (s is UpdateState.Available) {
                            Text("New version available: ${s.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AstraSuccess,
                                fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (s is UpdateState.Error) {
                            Text(s.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(
                            onClick = {
                                if (s is UpdateState.Available)
                                    updateVm.downloadAndInstall()
                                else
                                    updateVm.checkForUpdate()
                            },
                        ) {
                            Text(
                                if (s is UpdateState.Available)
                                    "Download & Install"
                                else "Check for Updates",
                            )
                        }
                    }

                    is UpdateState.Checking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Checking…",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    is UpdateState.Downloading -> {
                        val pct = s.progress
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("$pct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AstraOnSurfaceMuted)
                    }

                    is UpdateState.Verifying -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Verifying checksum & signature…",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    is UpdateState.Installing -> {
                        Text("Launching installer…",
                            style = MaterialTheme.typography.bodySmall)
                    }

                    is UpdateState.Success -> {
                        Text("Update completed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AstraSuccess)
                    }
                }
            }
        }

        // ══════════════ 备份 ══════════════
        item {
            AstraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Backup, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Backup & Restore",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))

                Text("Includes: module registry, trusted developer keys, " +
                    "privileged command audit log, key settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted)
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        exportLauncher.launch(
                            "astraveil-backup-${System.currentTimeMillis()}.zip")
                    }) {
                        Icon(Icons.Filled.FileDownload, null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export")
                    }
                    TextButton(onClick = {
                        importLauncher.launch(arrayOf("application/zip"))
                    }) {
                        Icon(Icons.Filled.FileUpload, null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore")
                    }
                }

                backupStatus?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.contains("failed")) AstraWarning else AstraSuccess)
                }
            }
        }
    }
}

// ── backup I/O helpers ──

private fun exportBackup(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(os).use { zip ->
                addFile(zip, "registry.json",
                    File(context.filesDir, "astra_modules/.registry.json"))
                addFile(zip, "trusted_developers.json",
                    File(context.filesDir, "trusted_developers.json"))
                addFile(zip, "command_audit.jsonl",
                    File(context.filesDir, "command_audit.jsonl"))
                addBytes(zip, "backup_meta.json",
                    """{"app":"AstraVeil","timestamp":${System.currentTimeMillis()}}"""
                        .toByteArray())
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}

private fun importBackup(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { ins ->
            ZipInputStream(ins).use { zip ->
                var entry = zip.nextEntry
                var sawMeta = false
                while (entry != null) {
                    when (entry.name) {
                        "backup_meta.json" -> sawMeta = true
                        "registry.json" -> writeTo(zip,
                            File(context.filesDir, "astra_modules/.registry.json"))
                        "trusted_developers.json" -> writeTo(zip,
                            File(context.filesDir, "trusted_developers.json"))
                        "command_audit.jsonl" -> writeTo(zip,
                            File(context.filesDir, "command_audit.jsonl"))
                    }
                    entry = zip.nextEntry
                }
                if (!sawMeta) return false
            }
        } ?: return false
        true
    } catch (e: Exception) {
        false
    }
}

private fun addFile(zip: ZipOutputStream, name: String, file: File) {
    if (!file.exists()) return
    zip.putNextEntry(ZipEntry(name))
    file.inputStream().use { it.copyTo(zip) }
    zip.closeEntry()
}

private fun addBytes(zip: ZipOutputStream, name: String, bytes: ByteArray) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(bytes)
    zip.closeEntry()
}

private fun writeTo(zip: ZipInputStream, file: File) {
    file.parentFile?.mkdirs()
    file.outputStream().use { os ->
        val buf = ByteArray(8192)
        var n = zip.read(buf)
        while (n > 0) { os.write(buf, 0, n); n = zip.read(buf) }
    }
}
