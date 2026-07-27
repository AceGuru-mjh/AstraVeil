package com.astraveil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraveil.app.ui.design.GlassCard
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.viewmodel.StatusViewModel

@Composable
fun DiagnosticsScreen(viewModel: StatusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Generate diagnostic report on launch
    LaunchedEffect(Unit) {
        viewModel.generateAndExportDiagnosticReport()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader() }
        item { DiagnosticActions(viewModel, state) }

        item { SectionLabel("System Recommendations") }
        item { SystemRecommendationsCard(state) }

        item { SectionLabel("Report Terminal Output (.astra-report)") }
        item { ConsoleReportView(state) }
    }
}

@Composable
private fun ScreenHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Diagnostics",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Analyze device capability integrity, trace warnings, and export professional reports for bug tracking.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = AstraAccent,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun DiagnosticActions(
    viewModel: StatusViewModel,
    state: StatusViewModel.UiState
) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "System Health Scanner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Generate a diagnostic report capturing kernel, SELinux, and provider states. The report is exported locally as 'diagnostics.astra-report'.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.generateAndExportDiagnosticReport() },
                    enabled = !state.exportingReport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstraAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (state.exportingReport) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("  Scanning…")
                    } else {
                        Icon(Icons.Filled.BugReport, null, modifier = Modifier.size(18.dp))
                        Text("  Run Full Scan")
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemRecommendationsCard(state: StatusViewModel.UiState) {
    val manufacturer = state.deviceProfile.manufacturer.lowercase()
    val isSamsung = manufacturer.contains("samsung")
    val isXiaomi = manufacturer.contains("xiaomi")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecommendationRow(
                icon = Icons.Filled.CheckCircle,
                tint = AstraSuccess,
                title = "Use AstraRoot Brokered Mode",
                description = "Enables fine-grained permission control and prevents legacy raw su execution flaws."
            )

            if (isSamsung) {
                RecommendationRow(
                    icon = Icons.Filled.Info,
                    tint = AstraWarning,
                    title = "Knox Restriction Bypass",
                    description = "Samsung Knox enforces secure boot checks. Ensure Custom ROM signature spoofing or KnoxPatch is active."
                )
            } else if (isXiaomi) {
                RecommendationRow(
                    icon = Icons.Filled.Info,
                    tint = AstraWarning,
                    title = "Mount Namespace Fix",
                    description = "HyperOS limits mount propagations. Disable 'Mount Namespace Separation' in your Root Manager if bind mounts fail."
                )
            } else {
                RecommendationRow(
                    icon = Icons.Filled.Info,
                    tint = AstraAccent,
                    title = "Optimize OverlayFS",
                    description = "For reliable system module mounts, ensure system partition is remounted as read-write or use overlayfs helpers."
                )
            }
        }
    }
}

@Composable
private fun RecommendationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConsoleReportView(state: StatusViewModel.UiState) {
    val reportText = state.diagnosticReport ?: "No report generated yet. Run a scan."
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = reportText,
                color = Color(0xFF00FFCC),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            )
        }
    }
}
