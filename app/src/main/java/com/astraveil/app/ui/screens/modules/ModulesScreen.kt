package com.astraveil.app.ui.screens.modules

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astraveil.app.modules.BuiltinModule
import com.astraveil.app.ui.AstraStrings
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
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
    // Use */* so ANY file manager / provider lets the user pick a .avm file.
    // The Trust Pipeline (AvmManifestParser) validates the archive on
    // selection — if the user picks a non-.avm file, the scan fails
    // gracefully with a PreviewError and the UI shows a snackbar.
    //
    // The previous restriction to ["application/zip","application/octet-stream"]
    // broke on many devices: file managers don't recognise the .avm
    // extension and refuse to show any files, making the + button look dead.
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            // Run the Trust Pipeline BEFORE showing any dialog (PR18.3)
            modulesViewModel.previewUri(uri)
        }
    }

    val launchPicker = {
        filePickerLauncher.launch(arrayOf("*/*"))
    }

    // ---- Snackbar for install operation state (Patch 18.2.3) ----
    LaunchedEffect(state.installState) {
        when (val s = state.installState) {
            is ModuleOperationState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                modulesViewModel.clearInstallState()
            }
            is ModuleOperationState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                modulesViewModel.clearInstallState()
            }
            ModuleOperationState.Idle, ModuleOperationState.Loading -> Unit
        }
    }

    // ---- Snackbar for scan failures ----
    LaunchedEffect(previewState) {
        if (previewState is PreviewState.Failed) {
            val reason = (previewState as PreviewState.Failed).reason
            snackbarHostState.showSnackbar(AstraStrings.cannotScanModule(reason))
            modulesViewModel.cancelPreview()
        }
    }

    // ---- Reboot-required notice for built-in bypass modules ----
    LaunchedEffect(state.rebootRequired) {
        if (state.rebootRequired) {
            snackbarHostState.showSnackbar("Bypass module toggled - reboot to take effect")
            modulesViewModel.clearRebootFlag()
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
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                        )
                    }
                }
            }

            // == BUILT-IN MODULES (Environment Shield) ==
            if (!state.loading && state.builtinModules.isNotEmpty()) {
                item { SectionTitle("BUILT-IN", state.builtinModules.size) }
                items(items = state.builtinModules, key = { "builtin_${it.id}" }) { module ->
                    BuiltinModuleCard(
                        module = module,
                        onToggle = { enabled ->
                            modulesViewModel.toggleBuiltin(module, enabled)
                        },
                    )
                }
            }

            if (state.modules.isNotEmpty()) {
                item { SectionTitle(AstraStrings.modInstalled, state.modules.size) }
                items(items = state.modules, key = { it.id }) { module ->
                    val op = state.moduleOperations[module.id]
                    ModuleCard(
                        module = module,
                        operation = op,
                        onStart = { modulesViewModel.start(module.id) },
                        onStop = { modulesViewModel.stop(module.id) },
                        onUninstall = { modulesViewModel.uninstall(module.id) },
                        onClearOp = { modulesViewModel.clearModuleOp(module.id) },
                    )
                }
            }

            if (!state.loading && state.modules.isEmpty()) {
                item { EmptyModulesCard() }
            }

            item { AvmFormatCard() }

            item {
                InstallCtaCard(
                    onClick = launchPicker,
                )
            }
        }

        // ---- FAB ----
        FloatingActionButton(
            onClick = launchPicker,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Install AVM Module")
        }

        // ---- Scanning indicator (overlay) ----
        if (previewState is PreviewState.Previewing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = AstraStrings.modScanningPackage,
                        style = MaterialTheme.typography.labelMedium,
                        color = AstraOnSurfaceMuted,
                    )
                }
            }
        }

        // ---- Snackbar ----
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        )
    }

    // ---- Security review dialog (uses real TrustReport) ----
    if (previewState is PreviewState.Ready) {
        val ready = previewState as PreviewState.Ready
        SecurityReviewDialog(
            report = ready.report,                  // ← full trust report
            installState = state.installState,
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
            text = AstraStrings.modTitle,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = AstraStrings.modSubtitle,
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
            text = AstraStrings.modEmptyTitle,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = AstraStrings.modEmptyHint,
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
            text = AstraStrings.modInstallCtaTitle,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = AstraStrings.modInstallCtaDesc,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = onClick) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(AstraStrings.modInstallFileBtn)
        }
    }
}

@Composable
private fun ModuleCard(
    module: ModuleInfo,
    operation: ModuleOperationState?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onUninstall: () -> Unit,
    onClearOp: () -> Unit,
) {
    // P2-17: a running module is ALIVE → LIQUID tier with teal accent.
    // Idle/installed/stopped/failed modules stay on the quiet CONTENT tier.
    val isRunning = module.state == ModuleUiState.RUNNING
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        tier = if (isRunning) SurfaceTier.LIQUID else SurfaceTier.CONTENT,
        accent = if (isRunning) AstraTeal.copy(alpha = 0.08f) else Color.Transparent,
        contentPadding = 16.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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

            // Permission preview — risk is nullable; "Unknown" when undeclared.
            ModulePermissionPreview(permissions = module.permissions)

            // ---- Operation feedback (Patch 18.2.3) ----
            OperationFeedbackRow(operation, onClearOp)

            // Phase 0 honesty: module loaded in app process, not isolated/rooted
            if (module.state == ModuleUiState.RUNNING) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Loaded in app process · Phase 0 (not isolated, not rooted)",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraWarning,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val busy = operation is ModuleOperationState.Loading
                when (module.state) {
                    ModuleUiState.RUNNING -> {
                        OutlinedButton(onClick = onStop, enabled = !busy) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(AstraStrings.actionStop)
                        }
                    }
                    ModuleUiState.INSTALLED,
                    ModuleUiState.STOPPED -> {
                        OutlinedButton(onClick = onStart, enabled = !busy) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(AstraStrings.actionStart)
                        }
                    }
                    ModuleUiState.FAILED -> {
                        Text(
                            text = AstraStrings.modFailedReinstall,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onUninstall, enabled = !busy) {
                    Text(AstraStrings.actionUninstall)
                }
            }
        }
    }
}

/**
 * Inline feedback line for the last start/stop/uninstall operation on
 * a module. Patch 18.2.3: turns silent Boolean flags into explicit
 * Loading / Success / Error text the user can read.
 */
@Composable
private fun OperationFeedbackRow(
    operation: ModuleOperationState?,
    onClear: () -> Unit,
) {
    when (operation) {
        null, ModuleOperationState.Idle -> Unit
        ModuleOperationState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = AstraStrings.modWorking,
                style = MaterialTheme.typography.labelSmall,
                color = AstraOnSurfaceMuted,
            )
        }
        is ModuleOperationState.Success -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = AstraTeal.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable { onClear() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = operation.message,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AstraTeal,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = AstraStrings.modDismiss,
                style = MaterialTheme.typography.labelSmall,
                color = AstraOnSurfaceMuted,
            )
        }
        is ModuleOperationState.Error -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable { onClear() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = operation.message,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = AstraStrings.modDismiss,
                style = MaterialTheme.typography.labelSmall,
                color = AstraOnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun StatePill(state: ModuleUiState) {
    val (label, color) = when (state) {
        ModuleUiState.RUNNING -> AstraStrings.stateRunning to AstraTeal
        ModuleUiState.INSTALLED -> AstraStrings.stateInstalled to AstraAccent
        ModuleUiState.STOPPED -> AstraStrings.stateStopped to AstraOnSurfaceMuted
        ModuleUiState.FAILED -> AstraStrings.stateFailed to MaterialTheme.colorScheme.error
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
                text = AstraStrings.modFormatTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = AstraStrings.modFormatDesc,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        FormatEntry(Icons.Filled.Code, "module.json", AstraStrings.modFormatManifestDesc)
        FormatEntry(Icons.Filled.Extension, "runtime/", AstraStrings.modFormatRuntimeDesc)
        FormatEntry(Icons.Filled.Inventory2, "assets/", AstraStrings.modFormatAssetsDesc)
        FormatEntry(Icons.Filled.Security, "permission.json", AstraStrings.modFormatPermissionDesc)
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

// == Built-in module card (Environment Shield toggles) ==

@Composable
private fun BuiltinModuleCard(
    module: BuiltinModule,
    onToggle: (Boolean) -> Unit,
) {
    val accentColor = when (module.category) {
        BuiltinModule.Category.CORE -> AstraAccent
        BuiltinModule.Category.SPOOF -> AstraTeal
        BuiltinModule.Category.BYPASS -> AstraWarning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (module.enabled) {
                accentColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Icon(
                imageVector = module.icon,
                contentDescription = null,
                tint = if (module.enabled) accentColor else AstraOnSurfaceMuted,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(14.dp))

            // Text
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (module.requiresReboot) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.RestartAlt,
                            contentDescription = "Requires reboot",
                            tint = AstraWarning,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Toggle
            Switch(
                checked = module.enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}
