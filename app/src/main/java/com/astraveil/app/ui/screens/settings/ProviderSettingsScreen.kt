package com.astraveil.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.providers.ProviderRegistry
import com.astraveil.providers.RootInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provider settings — lists every registered [com.astraveil.providers.RootProvider]
 * with a per-provider availability probe, and highlights the active backend
 * returned by [ProviderRegistry.detectActive].
 *
 * This screen is read-only at the API level (it does NOT switch the active
 * provider — that requires restarting the core, which is out of scope for
 * Phase 1). It surfaces what's detected so the user understands the current
 * backend state.
 */
@Composable
fun ProviderSettingsScreen() {
    val providers = remember { ProviderRegistry.all() }
    val available = remember { mutableStateMapOf<String, Boolean>() }
    var activeInfo by remember { mutableStateOf<RootInfo?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Probe each provider in parallel via runCatching so a slow
            // or throwing `available()` doesn't block the screen.
            providers.forEach { provider ->
                val ok = runCatching { provider.available() }.getOrDefault(false)
                available[provider.id] = ok
            }
            activeInfo = runCatching { ProviderRegistry.detectActive() }.getOrNull()
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
                    text = "Provider",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Detected root backends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Active Backend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                val info = activeInfo
                if (info == null || !info.detected) {
                    Text(
                        text = "No active root backend detected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AstraWarning,
                    )
                    Text(
                        text = "AstraVeil runs in capability-probe mode without a provider.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstraOnSurfaceMuted,
                    )
                } else {
                    Text(
                        text = info.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AstraSuccess,
                    )
                    Text(
                        text = "id: ${info.providerName}  ·  v${info.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstraOnSurfaceMuted,
                    )
                    if (info.modulePath.isNotBlank()) {
                        Text(
                            text = "modules: ${info.modulePath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AstraOnSurfaceMuted,
                        )
                    }
                }
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Registered Providers (${providers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.padding(top = 4.dp))
                providers.forEach { provider ->
                    val isActive = activeInfo?.providerName == provider.id
                    val isAvailable = available[provider.id] == true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) AstraAccent else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "id: ${provider.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AstraOnSurfaceMuted,
                            )
                        }
                        Text(
                            text = when {
                                isActive -> "Active"
                                isAvailable -> "Detected"
                                else -> "Unavailable"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isActive -> AstraAccent
                                isAvailable -> AstraSuccess
                                else -> AstraOnSurfaceMuted
                            },
                        )
                    }
                }
            }
        }
    }
}
