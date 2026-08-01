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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
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
import com.astraveil.app.ui.AstraStrings
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.core.modules.model.PermissionDeclaration
import com.astraveil.core.modules.security.ManifestStatus
import com.astraveil.core.modules.security.RiskLevel
import com.astraveil.core.modules.security.RiskSource
import com.astraveil.core.modules.security.SignatureStatus
import com.astraveil.core.modules.security.TrustReport
import com.astraveil.modules.compatibility.CompatibilityReport

/**
 * Pre-install Security Review dialog (PR18.3).
 *
 * Replaces the old `InstallModuleDialog`. Renders a full
 * [TrustReport] — package fingerprint, manifest status, permission
 * list with declared risks, overall risk level, and signature
 * status — BEFORE the user confirms install.
 *
 * Layout:
 * ```
 * ┌─────────────────────────────────────────────┐
 * │  📦 Astra Security Scan                     │
 * │                                             │
 * │  Module Name                                │
 * │  v1.0.0 · com.example.mod                  │
 * │  Description text...                        │
 * │                                             │
 * │  🔒 Package fingerprint                     │
 * │  a8f3...92c                                 │
 * │                                             │
 * │  🛡 Permissions (3)                         │
 * │  ● root_execution    High · 90              │
 * │  ● filesystem        Unknown                │
 * │  ● network           Medium · 40            │
 * │                                             │
 * │  Overall risk: HIGH                         │
 * │  Risk source: manifest                      │
 * │  Signature: Unknown                         │
 * │                                             │
 * │              [Cancel]  [Install…]           │
 * └─────────────────────────────────────────────┘
 * ```
 *
 * @param report       The TrustReport from the scan pipeline.
 * @param installState Drives the confirm button (Loading → spinner +
 *                     "Installing…", Error → inline error).
 */
@Composable
fun SecurityReviewDialog(
    report: TrustReport,
    installState: ModuleOperationState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    compatibilityReport: CompatibilityReport? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        // P2-17: dialog surface is static content → CONTENT tier.
        AstraCard(
            modifier = Modifier.fillMaxWidth(),
            tier = SurfaceTier.CONTENT,
            cornerRadius = 24.dp,
            contentPadding = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ---- Title ----
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.InstallMobile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = AstraStrings.secDialogTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // ---- Module identity (from manifest, if parsed) ----
                val preview = report.preview
                if (preview != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = preview.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "v${preview.version} · ${preview.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AstraOnSurfaceMuted,
                        )
                        if (preview.description.isNotBlank()) {
                            Text(
                                text = preview.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Text(
                        text = AstraStrings.manifestCouldNotBeRead(report.manifestStatus.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AstraError,
                    )
                }

                // ---- Package fingerprint (SHA-256) ----
                FingerprintRow(report.packageHash)

                // ---- Permissions ----
                if (preview != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                tint = AstraTeal,
                                modifier = Modifier.padding(end = 6.dp),
                            )
                            Text(
                                text = AstraStrings.permissionsCount(preview.permissions.size),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        PermissionDeclarationList(preview.permissions)
                    }
                }

                // ---- Trust summary block ----
                TrustSummary(report)

                // ---- Capability compatibility (if computed) ----
                if (compatibilityReport != null) {
                    CompatibilitySection(compatibilityReport)
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
                        Text(AstraStrings.secCancel)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = onConfirm,
                        enabled = report.isInstallable && installState !is ModuleOperationState.Loading,
                    ) {
                        if (installState is ModuleOperationState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(AstraStrings.secInstalling)
                        } else {
                            Text(AstraStrings.secInstall)
                        }
                    }
                }
            }
        }
    }
}

// ---- Sub-components ------------------------------------------------------

@Composable
private fun CompatibilitySection(report: CompatibilityReport) {
    val icon = if (report.compatible) Icons.Filled.Verified else Icons.Filled.Warning
    val tint = if (report.compatible) AstraSuccess else AstraWarning

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (report.compatible) "Compatible with this device"
                       else "Missing required capabilities",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = tint,
            )
        }

        if (!report.compatible && report.missing.isNotEmpty()) {
            report.missing.forEach { cap ->
                Text(
                    text = "✗ $cap — not available on this device",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraError,
                    modifier = Modifier.padding(start = 22.dp),
                )
            }
        }

        if (report.optionalMissing.isNotEmpty()) {
            Text(
                text = "Optional (not required): ${report.optionalMissing.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = AstraOnSurfaceMuted,
                modifier = Modifier.padding(start = 22.dp),
            )
        }
    }
}

@Composable
private fun FingerprintRow(hash: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = AstraStrings.secFingerprint,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = if (hash.isBlank()) AstraStrings.dash else hash,
            style = MaterialTheme.typography.bodySmall,
            color = AstraOnSurfaceMuted,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PermissionDeclarationList(permissions: List<PermissionDeclaration>) {
    if (permissions.isEmpty()) {
        Text(
            text = AstraStrings.secNoPermissions,
            style = MaterialTheme.typography.bodySmall,
            color = AstraOnSurfaceMuted,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        permissions.forEach { perm -> PermissionDeclarationRow(perm) }
    }
}

@Composable
private fun PermissionDeclarationRow(perm: PermissionDeclaration) {
    val riskColor = riskColor(perm.risk)
    val riskLabel = riskLabel(perm.risk)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = riskColor, shape = RoundedCornerShape(4.dp)),
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
                    null -> AstraStrings.riskUnknown
                    else -> "$riskLabel · $r"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = riskColor,
            )
        }
    }
}

@Composable
private fun TrustSummary(report: TrustReport) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Overall risk
        val (riskText, riskColor) = riskSummary(report.overallRiskLevel)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = AstraStrings.secOverallRisk,
                style = MaterialTheme.typography.labelMedium,
                color = AstraOnSurfaceMuted,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = riskText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = riskColor,
            )
        }

        // Risk source
        SummaryLine(
            label = AstraStrings.secRiskSourceLabel,
            value = when (report.riskSource) {
                RiskSource.MANIFEST -> AstraStrings.srcManifestDeclared
                RiskSource.UNDECLARED -> AstraStrings.srcUndeclared
                RiskSource.NONE -> AstraStrings.srcNone
            },
        )

        // Manifest status
        SummaryLine(
            label = AstraStrings.secManifestLabel,
            value = when (report.manifestStatus) {
                ManifestStatus.OK -> AstraStrings.manifestOk
                ManifestStatus.MISSING -> AstraStrings.manifestMissing
                ManifestStatus.MALFORMED -> AstraStrings.manifestMalformed
            },
        )

        // Signature status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Verified,
                contentDescription = null,
                tint = signatureColor(report.signatureStatus),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${AstraStrings.secSignatureLabel}: ${signatureLabel(report.signatureStatus)}",
                style = MaterialTheme.typography.labelSmall,
                color = AstraOnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = AstraOnSurfaceMuted,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---- helpers ----

private fun riskColor(risk: Int?): Color = when (risk) {
    null -> AstraOnSurfaceMuted
    else -> when {
        risk <= 30 -> AstraTeal
        risk <= 70 -> AstraWarning
        else -> AstraError
    }
}

private fun riskLabel(risk: Int?): String = when (risk) {
    null -> AstraStrings.riskUnknown
    else -> when {
        risk <= 30 -> AstraStrings.riskLow
        risk <= 70 -> AstraStrings.riskMedium
        else -> AstraStrings.riskHigh
    }
}

private fun riskSummary(level: RiskLevel): Pair<String, Color> = when (level) {
    RiskLevel.LOW -> AstraStrings.riskLevelLow to AstraTeal
    RiskLevel.MEDIUM -> AstraStrings.riskLevelMedium to AstraWarning
    RiskLevel.HIGH -> AstraStrings.riskLevelHigh to AstraError
    RiskLevel.CRITICAL -> AstraStrings.riskLevelCritical to AstraError
    RiskLevel.UNKNOWN -> AstraStrings.riskLevelUnknown to AstraOnSurfaceMuted
}

private fun signatureColor(status: SignatureStatus): Color = when (status) {
    SignatureStatus.VERIFIED -> AstraSuccess
    SignatureStatus.REJECTED -> AstraError
    SignatureStatus.UNVERIFIED -> AstraWarning
    SignatureStatus.UNSIGNED, SignatureStatus.UNKNOWN -> AstraOnSurfaceMuted
}

private fun signatureLabel(status: SignatureStatus): String = when (status) {
    SignatureStatus.UNKNOWN -> AstraStrings.sigUnknown
    SignatureStatus.UNSIGNED -> AstraStrings.sigUnsigned
    SignatureStatus.UNVERIFIED -> AstraStrings.sigUnverified
    SignatureStatus.VERIFIED -> AstraStrings.sigVerified
    SignatureStatus.REJECTED -> AstraStrings.sigRejected
}
