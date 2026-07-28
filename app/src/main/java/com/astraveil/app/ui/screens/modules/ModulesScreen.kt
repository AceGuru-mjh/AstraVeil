package com.astraveil.app.ui.screens.modules

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astraveil.app.ui.design.AstraGlass
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.core.modules.model.ModuleInfo
import com.astraveil.core.modules.model.ModuleUiState

@Composable
fun ModulesScreen(
    modulesViewModel: ModulesViewModel = viewModel(),
) {
    val state by modulesViewModel.uiState.collectAsStateWithLifecycle()
    val previewState by modulesViewModel.previewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- File picker ----
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            // NEW: pre-parse BEFORE showing any dialog
            modulesViewModel.previewUri(uri)
        }
    }

    // ---- Snackbar for errors / success ----
    LaunchedEffect(state.error, state.successMessage) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            modulesViewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            modulesViewModel.clearMessages()
        }
    }

    // ---- Snackbar for preview failures ----
    LaunchedEffect(previewState) {
        if (previewState is PreviewState.Failed) {
            val reason = (previewState as PreviewState.Failed).reason
            snackbarHostState.showSnackbar("Cannot preview module: $reason")
            modulesViewModel.cancelPreview()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ModulesHeader() }

            if (state.loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = AstraGlass.Glow,
                            strokeWidth = 2.5.dp,
                        )
                    }
                }
            }

            if (state.modules.isNotEmpty()) {
                item { SectionTitle("Installed Modules", state.modules.size) }
                items(items = state.modules, key = { it.id }) { module ->
                    ModuleCard(
                        module = module,
                        onStart = { modulesViewModel.start(module.id) },
                        onStop = { modulesViewModel.stop(module.id) },
                        onUninstall = { modulesViewModel.uninstall(module.id) },
                    )
                }
            }

            if (!state.loading && state.modules.isEmpty()) {
                item { EmptyModulesCard() }
            }

            item { AvmFormatCard() }

            item {
                InstallCtaCard(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                )
            }
        }

        // ---- FAB ----
        FloatingActionButton(
            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = AstraGlass.Glow,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Install AVM Module")
        }

        // ---- Previewing indicator (overlay) ----
        if (previewState is PreviewState.Previewing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = AstraGlass.Glow,
                    strokeWidth = 3.dp,
                )
            }
        }

        // ---- Snackbar ----
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        )
    }

    // ---- Install confirmation dialog (FIXED: uses real preview data) ----
    if (previewState is PreviewState.Ready) {
        val ready = previewState as PreviewState.Ready
        InstallModuleDialog(
            modulePreview = ready.module,   // ← REAL DATA, not null
            installing = state.installing,
            onConfirm = { modulesViewModel.confirmInstall() },
            onDismiss = { modulesViewModel.cancelPreview() },
        )
    }
}

// ---- Composable pieces ----------------------------------------------------

@Composable
private fun ModulesHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Modules",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Astra Modules extend AstraVeil with isolated, permissioned packages.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SectionTitle(label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            color = AstraOnSurfaceMuted,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun EmptyModulesCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AstraAccent.copy(alpha = 0.06f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(
                color = AstraAccent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(20.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Inventory2,
                contentDescription = null,
                tint = AstraAccent,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            text = "No Astra Modules installed yet",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Tap + or \"Install a .avm file\" to get started.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun InstallCtaCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AstraTeal.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Install an Astra Module",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Pick a .avm file. AstraVeil will pre-parse its manifest and show you the real module name, version, and requested permissions before installing.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = onClick) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Install a .avm file")
        }
    }
}

@Composable
private fun ModuleCard(
    module: ModuleInfo,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onUninstall: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(
                    color = AstraGlass.Glow.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Extension,
                    contentDescription = null,
                    tint = AstraGlass.Glow,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "v${module.version} · ${module.id}",
                    color = AstraOnSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            StatePill(module.state)
        }

        if (module.description.isNotBlank()) {
            Text(
                text = module.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Permission preview (reuse the same renderer as the install dialog)
        ModulePermissionPreview(permissions = module.permissions)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (module.state) {
                ModuleUiState.RUNNING -> {
                    OutlinedButton(onClick = onStop) {
                        Icon(Icons.Filled.Stop, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
                ModuleUiState.INSTALLED,
                ModuleUiState.STOPPED -> {
                    OutlinedButton(onClick = onStart) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start")
                    }
                }
                ModuleUiState.FAILED -> {
                    Text(
                        text = "Module failed — reinstall required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onUninstall) {
                Text("Uninstall")
            }
        }
    }
}

@Composable
private fun StatePill(state: ModuleUiState) {
    val (label, color) = when (state) {
        ModuleUiState.RUNNING -> "Running" to AstraTeal
        ModuleUiState.INSTALLED -> "Installed" to AstraAccent
        ModuleUiState.STOPPED -> "Stopped" to AstraOnSurfaceMuted
        ModuleUiState.FAILED -> "Failed" to MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun AvmFormatCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FolderZip, contentDescription = null, tint = AstraAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "The .avm format",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = "An Astra Module is a signed .avm bundle that contains everything AstraVeil needs to safely install and run an extension:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        FormatEntry(Icons.Filled.Code, "module.json", "Manifest: id, version, entrypoint, dependencies.")
        FormatEntry(Icons.Filled.Extension, "runtime/", "Kotlin/Native or DEX code, executed inside the AstraVM sandbox.")
        FormatEntry(Icons.Filled.Inventory2, "assets/", "Static resources shipped with the module.")
        FormatEntry(Icons.Filled.Security, "permission.json", "Capability grants requested by the module (Mount, Hook, Namespace, …).")
    }
}

@Composable
private fun FormatEntry(icon: ImageVector, name: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).background(
                color = AstraTeal.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AstraTeal, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
