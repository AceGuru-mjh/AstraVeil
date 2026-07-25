package com.astraveil.app.ui.screens

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraveil.app.ui.components.StatusPill
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.viewmodel.StatusViewModel

/**
 * The Provider abstraction screen.
 *
 * Lists the four AstraVeil root backends (Magisk, KernelSU, APatch,
 * AstraRoot) and shows which (if any) was detected. Phase 0 expectation:
 * none active — a hint is shown explaining this is normal.
 */
@Composable
fun ProviderScreen(viewModel: StatusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val providers = providerCatalog()
    val activeName = state.providerName

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader() }

        if (activeName == "None") {
            item { NoProviderHint() }
        }

        item { SectionLabel("Backend abstraction layer") }

        items(providers, key = { it.name }) { provider ->
            ProviderCard(
                provider = provider,
                active = provider.matches(activeName),
                version = if (provider.matches(activeName)) state.providerVersion else null
            )
        }
    }
}

@Composable
private fun ScreenHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Provider",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "AstraVeil abstracts over Magisk, KernelSU, APatch and its own AstraRoot backend.",
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
private fun NoProviderHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = AstraWarning,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "No root backend detected",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "This is expected during Phase 0. AstraVeil runs in capability-probe mode without an active provider.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: ProviderDescriptor,
    active: Boolean,
    version: String?
) {
    val accent = if (active) AstraSuccess else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = provider.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = provider.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                if (active && version != null) {
                    Text(
                        text = "Version $version",
                        color = AstraSuccess,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            StatusPill(
                text = if (active) "Active" else "Not detected",
                color = if (active) AstraSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                icon = if (active) Icons.Filled.CheckCircle else null
            )
        }
    }
}

// --------------------------------------------------------------------- model

private data class ProviderDescriptor(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val aliases: Set<String> = emptySet()
) {
    fun matches(detected: String): Boolean =
        detected.equals(name, ignoreCase = true) ||
            detected.lowercase() in aliases.map { it.lowercase() }
}

private fun providerCatalog(): List<ProviderDescriptor> = listOf(
    ProviderDescriptor(
        name = "Magisk",
        description = "The classic su daemon + MagiskHide/Zygisk framework.",
        icon = Icons.Filled.Terminal,
        aliases = setOf("magisk")
    ),
    ProviderDescriptor(
        name = "KernelSU",
        description = "In-kernel root solution integrated directly into the Linux kernel.",
        icon = Icons.Filled.Bolt,
        aliases = setOf("kernelsu", "ksu")
    ),
    ProviderDescriptor(
        name = "APatch",
        description = "Patch-based root that injects a su binary without modifying the kernel.",
        icon = Icons.Filled.Security,
        aliases = setOf("apatch")
    ),
    ProviderDescriptor(
        name = "AstraRoot",
        description = "AstraVeil's native root backend (Phase 2+) — deep control plane.",
        icon = Icons.Filled.Cloud,
        aliases = setOf("astraroot", "astra")
    )
)
