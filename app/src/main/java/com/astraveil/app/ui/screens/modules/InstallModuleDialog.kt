package com.astraveil.app.ui.screens.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.astraveil.app.repository.ModulePreview
import com.astraveil.app.repository.PermissionPreview
import com.astraveil.app.ui.design.AstraGlass
import com.astraveil.app.ui.design.GlassSurface
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning

/**
 * Pre-install confirmation dialog (Patch 18.2.1 + 18.2.3).
 *
 * ALWAYS receives a real [ModulePreview] from the [ModuleInspector]
 * pre-parse step — never placeholder text. Risk badges reflect the
 * manifest truthfully: declared levels are shown, undeclared ones are
 * rendered as "Unknown".
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
 * │  ● root_execution    High · 90          │
 * │  ● filesystem        Unknown            │
 * │                                         │
 * │  Overall risk: High / Unknown           │
 * │                                         │
 * │              [Cancel]  [Install…]       │
 * └─────────────────────────────────────────┘
 * ```
 *
 * @param installState Drives the confirm button label & enabled state
 *                     (Idle → "Install", Loading → "Installing…" + spinner,
 *                      Error → "Install" + error shown).
 */
@Composable
fun InstallModuleDialog(
    modulePreview: ModulePreview,
    installState: ModuleOperationState,
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
                            contentDescription = null,
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

                    PreviewPermissionList(modulePreview.permissions)

                    // ---- Overall risk summary ----
                    val overall = overallRisk(modulePreview.permissions)
                    Text(
                        text = overall.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = overall.color,
                    )
                }

                // ---- Install error (if any) ----
                if (installState is ModuleOperationState.Error) {
                    Text(
                        text = installState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = AstraError,
                    )
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
                        enabled = installState !is ModuleOperationState.Loading,
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    TextButton(
                        onClick = onConfirm,
                        enabled = installState !is ModuleOperationState.Loading,
                    ) {
                        if (installState is ModuleOperationState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = AstraGlass.Glow,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Installing…")
                        } else {
                            Text("Install")
                        }
                    }
                }
            }
        }
    }
}

// ---- Helpers ----

@Composable
private fun PreviewPermissionList(permissions: List<PermissionPreview>) {
    if (permissions.isEmpty()) {
        Text(
            text = "No permissions requested.",
            style = MaterialTheme.typography.bodySmall,
            color = AstraOnSurfaceMuted,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        permissions.forEach { perm -> PreviewPermissionRow(perm) }
    }
}

@Composable
private fun PreviewPermissionRow(perm: PermissionPreview) {
    val riskColor = previewRiskColor(perm.risk)
    val riskLabel = previewRiskLabel(perm.risk)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = riskColor,
                    shape = RoundedCornerShape(4.dp),
                ),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = perm.capability,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (perm.reason.isNotBlank()) {
                Text(
                    text = perm.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .background(
                    color = riskColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = when (val r = perm.risk) {
                    null -> "Unknown"
                    else -> "$riskLabel · $r"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = riskColor,
            )
        }
    }
}

private data class OverallRisk(val label: String, val color: Color)

private fun overallRisk(permissions: List<PermissionPreview>): OverallRisk {
    val declared = permissions.mapNotNull { it.risk }
    if (declared.isEmpty()) return OverallRisk("Overall risk: Unknown", AstraOnSurfaceMuted)
    val maxRisk = declared.max()
    return when {
        maxRisk <= 30 -> OverallRisk("Overall risk: Low", AstraTeal)
        maxRisk <= 70 -> OverallRisk("Overall risk: Medium", AstraWarning)
        else -> OverallRisk("Overall risk: High", AstraError)
    }
}

private fun previewRiskColor(risk: Int?): Color = when (risk) {
    null -> AstraOnSurfaceMuted
    else -> when {
        risk <= 30 -> AstraTeal
        risk <= 70 -> AstraWarning
        else -> AstraError
    }
}

private fun previewRiskLabel(risk: Int?): String = when (risk) {
    null -> "Unknown"
    else -> when {
        risk <= 30 -> "Low"
        risk <= 70 -> "Medium"
        else -> "High"
    }
}

// ---- end of file ----
