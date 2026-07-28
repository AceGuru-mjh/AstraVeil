package com.astraveil.app.ui.screens.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.astraveil.app.ui.design.AstraGlass
import com.astraveil.app.ui.design.GlassSurface
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.core.modules.model.ModuleInfo
import com.astraveil.core.modules.model.RiskSource

/**
 * Pre-install confirmation dialog.
 *
 * ALWAYS receives a real [ModuleInfo] from the pre-parse step.
 * Never shows placeholder / generic text.
 *
 * Layout:
 * ```
 * ┌─────────────────────────────────────────┐
 * │  📦 Install AVM Module                  │
 * │                                         │
 * │  Module Name                            │
 * │  v1.0.0 · com.example.mod              │
 * │  Description text...                    │
 * │                                         │
 * │  🛡 Requested Permissions               │
 * │  ● root_execution    High · 90  [manifest] │
 * │  ● filesystem        Low · 30   [estimated]│
 * │                                         │
 * │  Overall risk: High                     │
 * │  Risk data: 1 from manifest, 1 estimated│
 * │                                         │
 * │              [Cancel]  [Install]        │
 * └─────────────────────────────────────────┘
 * ```
 */
@Composable
fun InstallModuleDialog(
    modulePreview: ModuleInfo,
    installing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(cornerRadius = 24) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ---- Title ----
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.InstallMobile,
                        contentDescription = null,
                        tint = AstraGlass.Glow,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Install AVM Module",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // ---- Module identity (REAL DATA from pre-parse) ----
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = modulePreview.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "v${modulePreview.version} · ${modulePreview.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AstraOnSurfaceMuted,
                    )
                    if (modulePreview.description.isNotBlank()) {
                        Text(
                            text = modulePreview.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // ---- Permissions (REAL DATA) ----
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Shield,
                            null,
                            tint = AstraTeal,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(
                            text = "Requested Permissions",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    ModulePermissionPreview(
                        permissions = modulePreview.permissions,
                    )

                    // ---- Risk summary ----
                    val maxRisk = modulePreview.permissions
                        .maxOfOrNull { it.risk } ?: 0
                    val riskSummary = when {
                        maxRisk <= 30 -> "Overall risk: Low"
                        maxRisk <= 70 -> "Overall risk: Medium"
                        else -> "Overall risk: High"
                    }
                    Text(
                        text = riskSummary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            maxRisk <= 30 -> AstraTeal
                            maxRisk <= 70 -> AstraWarning
                            else -> AstraError
                        },
                    )

                    // ---- Risk provenance (NEW) ----
                    val fromManifest = modulePreview.permissions
                        .count { it.riskSource == RiskSource.MANIFEST }
                    val estimated = modulePreview.permissions
                        .count { it.riskSource == RiskSource.ESTIMATED }
                    if (modulePreview.permissions.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Verified,
                                null,
                                tint = if (fromManifest > 0) AstraSuccess else AstraOnSurfaceMuted,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                            Text(
                                text = buildString {
                                    if (fromManifest > 0) append("$fromManifest from manifest")
                                    if (fromManifest > 0 && estimated > 0) append(", ")
                                    if (estimated > 0) append("$estimated estimated")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = AstraOnSurfaceMuted,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ---- Buttons ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !installing,
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    TextButton(
                        onClick = onConfirm,
                        enabled = !installing,
                    ) {
                        Text(if (installing) "Installing…" else "Install")
                    }
                }
            }
        }
    }
}
