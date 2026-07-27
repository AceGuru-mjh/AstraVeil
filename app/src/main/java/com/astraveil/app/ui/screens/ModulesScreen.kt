package com.astraveil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.viewmodel.StatusViewModel

@Composable
fun ModulesScreen(viewModel: StatusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showInstallDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ModulesScreenHeader() }
            item { EmptyState(state.modulesActive) }
            item { AvmFormatCard() }
        }

        FloatingActionButton(
            onClick = { showInstallDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Install AVM",
            )
        }
    }

    if (showInstallDialog) {
        ModuleInstallDialog(
            onDismiss = { showInstallDialog = false },
            onConfirm = { showInstallDialog = false },
        )
    }
}

@Composable
private fun ModulesScreenHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Modules",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Astra Modules extend AstraVeil with isolated, permissioned packages.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyState(activeCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).background(
                    color = AstraAccent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(20.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Inventory2, null, tint = AstraAccent, modifier = Modifier.size(38.dp))
            }
            Text("No Astra Modules installed yet", color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Tap + to install a .avm file.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            if (activeCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text("$activeCount module(s) reported active", color = AstraTeal,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AvmFormatCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FolderZip, null, tint = AstraAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("The .avm format", color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text("An Astra Module is a signed .avm bundle that contains everything AstraVeil needs to safely install and run an extension:",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            FormatEntry(Icons.Filled.Code, "module.json", "Manifest: id, version, entrypoint, dependencies.")
            FormatEntry(Icons.Filled.Extension, "runtime/", "Kotlin/Native or DEX code, executed inside the AstraVM sandbox.")
            FormatEntry(Icons.Filled.Inventory2, "assets/", "Static resources shipped with the module.")
            FormatEntry(Icons.Filled.Security, "permission.json", "Capability grants requested by the module (Mount, Hook, Namespace, …).")
        }
    }
}

@Composable
private fun FormatEntry(icon: ImageVector, name: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).background(
            color = AstraTeal.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)
        ), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AstraTeal, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ModuleInstallDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install AVM Module") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select a .avm file to install.", style = MaterialTheme.typography.bodyMedium)
                Text("The module will be validated and sandboxed before installation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Install") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
