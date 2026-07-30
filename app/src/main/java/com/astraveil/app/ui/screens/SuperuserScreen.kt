package com.astraveil.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.su.MagiskSuRepository
import com.astraveil.app.ui.design.AstraGlass
import com.astraveil.app.ui.design.LiquidGlass
import com.astraveil.app.ui.design.LiquidGlassCard
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Superuser management — REAL su policy management.
 *
 * Phase 0 (Magisk backend):
 *   Reads/writes Magisk's /data/adb/magisk.db policies table.
 *   When the user changes a policy here, it takes effect immediately:
 *   open Termux → type `su` → Magisk checks the database → uses the
 *   policy that AstraVeil set.
 */
@Composable
fun SuperuserScreen() {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var dbAvailable by remember { mutableStateOf(false) }
    var policies by remember { mutableStateOf<List<MagiskSuRepository.SuPolicyEntry>>(emptyList()) }
    var logs by remember { mutableStateOf<List<MagiskSuRepository.SuLogEntry>>(emptyList()) }
    var hasRoot by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Resolve the active provider (detectActive → byId) then build the repo.
    val repo = remember { mutableStateOf<MagiskSuRepository?>(null) }

    LaunchedEffect(Unit) {
        val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
        val provider = info?.let { ProviderRegistry.byId(it.providerName) }
        val providerAvailable = provider?.let { runCatching { it.available() }.getOrDefault(false) } ?: false
        if (provider == null || !providerAvailable) {
            // No root — this is NOT an error, just a precondition not met
            hasRoot = false
            loading = false
            return@LaunchedEffect
        }
        hasRoot = true
        val r = MagiskSuRepository(provider)
        repo.value = r

        val available = r.isAvailable()
        dbAvailable = available

        if (available) {
            policies = r.listPolicies()
            logs = r.listLogs(30)
        } else {
            // Root detected but DB not accessible — THIS is a real error
            errorMessage = "Root backend detected but the su policy " +
                "database is not accessible. Try rebooting or " +
                "reinstalling your root solution."
        }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- Header ----
        item {
            Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text(
                    text = "Superuser",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Real su policy management. Changes take effect immediately " +
                        "when any app calls su.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ---- Loading ----
        if (loading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
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

        // ---- State 1: No root — amber info card, NOT red error ----
        if (!loading && !hasRoot) {
            item { NoRootInfoCard() }
        }

        // ---- State 3: Root error (root detected but DB inaccessible) ----
        if (errorMessage != null) {
            item {
                LiquidGlassCard(accent = LiquidGlass.AccentError) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = AstraError,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // ---- Active policies ----
        if (dbAvailable && policies.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = AstraGlass.Glow,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Su Policies (${policies.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            items(
                items = policies,
                key = { it.uid },
            ) { entry ->
                SuPolicyCard(
                    entry = entry,
                    onPolicyChange = { newPolicy ->
                        scope.launch {
                            repo.value?.setPolicy(entry.uid, entry.packageName, newPolicy)
                            policies = repo.value?.listPolicies() ?: policies
                        }
                    },
                    onDelete = {
                        scope.launch {
                            repo.value?.deletePolicy(entry.uid)
                            policies = repo.value?.listPolicies() ?: policies
                        }
                    },
                )
            }
        }

        // ---- No policies ----
        if (dbAvailable && !loading && policies.isEmpty()) {
            item {
                LiquidGlassCard {
                    Text(
                        text = "No su policies yet. When an app requests su " +
                            "(e.g. Termux → type 'su'), Magisk will prompt you. " +
                            "The policy will appear here after the first request.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ---- Su request logs ----
        if (dbAvailable && logs.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = AstraGlass.Teal,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Recent Su Requests",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            items(
                items = logs,
                key = { "${it.fromUid}-${it.time}" },
            ) { log ->
                SuLogCard(log)
            }
        }
    }
}

// ================================================================
// State 1: No root — informative, NOT alarming
// ================================================================

@Composable
private fun NoRootInfoCard() {
    LiquidGlassCard(accent = LiquidGlass.AccentAmber) {
        // Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = AstraWarning,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Root Backend Required",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "This page manages su access policies for apps on your " +
                "device. It reads and writes your root backend's policy " +
                "database directly — changes take effect immediately.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Once a root backend is installed, you can:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        val features = listOf(
            "View all apps that have requested su access",
            "Grant, deny, or set \"ask\" per app",
            "See recent su request history with timestamps",
            "Changes apply instantly — no reboot needed",
        )
        features.forEach { feature ->
            Row(
                modifier = Modifier.padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "•",
                    color = AstraWarning,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Supported root backends:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        val backends = listOf(
            "Magisk" to "Most common. Patches boot image.",
            "KernelSU" to "Kernel-level. GKI compatible.",
            "APatch" to "Kernel patch. No boot image modification.",
            "AstraRoot" to "AstraVeil's own backend (coming soon).",
        )
        backends.forEach { (name, desc) ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AstraGlass.Glow,
                    modifier = Modifier.width(80.dp),
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
            }
        }
    }
}

// ---- Policy card ----

@Composable
private fun SuPolicyCard(
    entry: MagiskSuRepository.SuPolicyEntry,
    onPolicyChange: (MagiskSuRepository.SuPolicy) -> Unit,
    onDelete: () -> Unit,
) {
    val (statusIcon, statusColor, statusLabel) = when (entry.policy) {
        MagiskSuRepository.SuPolicy.ALLOW ->
            Triple(Icons.Filled.CheckCircle, AstraSuccess, "Allowed")
        MagiskSuRepository.SuPolicy.ASK ->
            Triple(Icons.Filled.HelpOutline, AstraWarning, "Ask")
        MagiskSuRepository.SuPolicy.DENY ->
            Triple(Icons.Filled.Block, AstraError, "Denied")
    }

    LiquidGlassCard(
        enablePressEffect = false,
        contentPadding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status icon
            Icon(
                statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp),
            )

            Spacer(Modifier.width(12.dp))

            // App info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                val untilSuffix = if (entry.until > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    " · until ${sdf.format(Date(entry.until * 1000))}"
                } else {
                    " · forever"
                }
                Text(
                    text = "uid ${entry.uid} · $statusLabel$untilSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Delete
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove policy",
                    tint = AstraError.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Policy selector row
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MagiskSuRepository.SuPolicy.entries.forEach { policy ->
                val selected = entry.policy == policy
                val color = when (policy) {
                    MagiskSuRepository.SuPolicy.ALLOW -> AstraSuccess
                    MagiskSuRepository.SuPolicy.ASK -> AstraWarning
                    MagiskSuRepository.SuPolicy.DENY -> AstraError
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected) color.copy(alpha = 0.18f)
                            else color.copy(alpha = 0.05f),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { if (!selected) onPolicyChange(policy) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = policy.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) color else color.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ---- Log card ----

@Composable
private fun SuLogCard(log: MagiskSuRepository.SuLogEntry) {
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    val allowed = log.action.contains("allow", ignoreCase = true)

    LiquidGlassCard(
        enablePressEffect = false,
        enableSpecular = false,
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (allowed) Icons.Filled.CheckCircle else Icons.Filled.Block,
                contentDescription = null,
                tint = if (allowed) AstraSuccess else AstraError,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.appName.ifBlank { log.packageName },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = "uid ${log.fromUid} · ${log.action}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted,
                )
            }
            Text(
                text = sdf.format(Date(log.time * 1000)),
                style = MaterialTheme.typography.labelSmall,
                color = AstraOnSurfaceMuted,
            )
        }
    }
}
