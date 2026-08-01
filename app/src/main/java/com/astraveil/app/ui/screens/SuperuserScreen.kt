package com.astraveil.app.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.astraveil.app.adb.AdbManager
import com.astraveil.app.su.MagiskSuRepository
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.screens.superuser.AdbConsoleCard
import com.astraveil.app.ui.screens.superuser.TerminalLauncherCard
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Superuser management — real functionality, no hollow shells.
 *
 * Sections (all real, all functional):
 *  1. Terminal launcher     → opens full terminal (ROOT/ADB/SHELL)
 *  2. ADB console           → embedded real command executor
 *  3. Su policies           → real Magisk DB read/write + add new
 *  4. Su request log        → real Magisk DB logs + per-app stats
 */
@Composable
fun SuperuserScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var loading by remember { mutableStateOf(true) }
    var dbAvailable by remember { mutableStateOf(false) }
    var hasRoot by remember { mutableStateOf(false) }
    var policies by remember {
        mutableStateOf<List<MagiskSuRepository.SuPolicyEntry>>(emptyList())
    }
    var logs by remember {
        mutableStateOf<List<MagiskSuRepository.SuLogEntry>>(emptyList())
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAppPicker by remember { mutableStateOf(false) }
    var lockdownSnapshot by remember {
        mutableStateOf<List<MagiskSuRepository.PolicySnapshot>?>(null)
    }
    var lockdownCount by remember { mutableStateOf<Int?>(null) }

    val repo = remember { mutableStateOf<MagiskSuRepository?>(null) }

    // ---- Load real data ----
    suspend fun loadData() {
        val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
        val provider = info?.let { ProviderRegistry.byId(it.providerName) }
        val providerAvailable = provider
            ?.let { runCatching { it.available() }.getOrDefault(false) }
            ?: false

        if (provider == null || !providerAvailable) {
            hasRoot = false
            loading = false
            return
        }

        hasRoot = true
        val r = MagiskSuRepository(provider)
        repo.value = r

        val available = r.isAvailable()
        dbAvailable = available

        if (available) {
            policies = r.listPolicies()
            logs = r.listLogs(50)
        } else {
            errorMessage = "Root backend detected but su policy database " +
                "is not accessible. Try rebooting."
        }
        loading = false
    }

    LaunchedEffect(Unit) { loadData() }

    // ---- Filtered policies ----
    val filtered = remember(searchQuery, policies) {
        if (searchQuery.isBlank()) policies
        else policies.filter {
            it.packageName.contains(searchQuery, ignoreCase = true) ||
                it.uid.toString().contains(searchQuery)
        }
    }

    // ---- Su usage stats (aggregated from logs) ----
    val suStats = remember(logs) { SuStatsAggregator.aggregate(logs) }

    // ---- Risk-based grouping ----
    val groups = remember(filtered, suStats) { SuRiskGrouper.group(filtered, suStats) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- Header + refresh ----
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Superuser",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Manage root access for apps on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = {
                    scope.launch {
                        loading = true
                        errorMessage = null
                        loadData()
                    }
                }) {
                    Icon(Icons.Filled.Refresh, "Refresh",
                        tint = MaterialTheme.colorScheme.primary)
                }
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
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                    )
                }
            }
        }

        // ---- No root: informative, not alarming ----
        if (!loading && !hasRoot) {
            item { NoRootInfoCard() }
        }

        // ---- Root error ----
        if (errorMessage != null) {
            item {
                AstraCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null,
                            tint = AstraError, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════
        // REAL TOOL 1: Terminal launcher (ACTION, not switch)
        // ══════════════════════════════════════════════════════════
        if (!loading) {
            item {
                TerminalLauncherCard(
                    onOpenTerminal = {
                        navController.navigate("terminal")
                    },
                )
            }
        }

        // ══════════════════════════════════════════════════════════
        // Panic Lockdown bar — set ALL policies to DENY (real Magisk DB)
        // ══════════════════════════════════════════════════════════
        if (dbAvailable && !loading) {
            item {
                LockdownBar(
                    lockdownCount = lockdownCount,
                    onLockdown = {
                        scope.launch {
                            val (snapshot, n) = repo.value?.lockdown()
                                ?: (emptyList<MagiskSuRepository.PolicySnapshot>() to 0)
                            lockdownSnapshot = snapshot
                            lockdownCount = n
                            policies = repo.value?.listPolicies() ?: policies
                            logs = repo.value?.listLogs(50) ?: logs
                        }
                    },
                    onRestore = {
                        scope.launch {
                            lockdownSnapshot?.let { repo.value?.restorePolicies(it) }
                            lockdownSnapshot = null
                            lockdownCount = null
                            policies = repo.value?.listPolicies() ?: policies
                            logs = repo.value?.listLogs(50) ?: logs
                        }
                    },
                    onKeepLocked = {
                        lockdownSnapshot = null
                        lockdownCount = null
                    },
                )
            }
        }

        // ══════════════════════════════════════════════════════════
        // REAL TOOL 2: Embedded ADB console (real execution)
        // ══════════════════════════════════════════════════════════
        if (!loading) {
            item { AdbConsoleCard() }
        }

        // ══════════════════════════════════════════════════════════
        // REAL TOOL 3: Su policy management
        // ══════════════════════════════════════════════════════════
        if (dbAvailable) {
            // ---- Search + Add ----
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search policies…") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { showAppPicker = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp),
                            ),
                    ) {
                        Icon(Icons.Filled.Add, "Add policy",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            // ---- Section header ----
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Su Policies (${filtered.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // ---- ADB Shell (uid 2000) special entry ----
            item {
                AdbShellPolicyCard(
                    policies = policies,
                    onPolicyChange = { newPolicy ->
                        scope.launch {
                            repo.value?.setPolicy(
                                AdbManager.SHELL_UID,
                                "com.android.shell",
                                newPolicy,
                            )
                            policies = repo.value?.listPolicies() ?: policies
                        }
                    },
                )
            }

            // ---- Policy cards (grouped by risk) ----
            groups.forEach { group ->
                item(key = "header-${group.id}") {
                    GroupHeader(group)
                }
                items(items = group.entries, key = { "${group.id}-${it.uid}" }) { entry ->
                    SuPolicyCard(
                        entry = entry,
                        stats = suStats[entry.uid],
                        appIcon = rememberAppIcon(entry.packageName),
                        onPolicyChange = { newPolicy ->
                            scope.launch {
                                repo.value?.setPolicy(
                                    entry.uid, entry.packageName, newPolicy,
                                    logging = entry.logging,
                                    notification = entry.notification,
                                )
                                policies = repo.value?.listPolicies() ?: policies
                            }
                        },
                        onGrantTemporary = { minutes ->
                            scope.launch {
                                repo.value?.grantTemporary(entry.uid, entry.packageName, minutes)
                                policies = repo.value?.listPolicies() ?: policies
                            }
                        },
                        onMakePermanent = {
                            scope.launch {
                                repo.value?.makePermanent(entry.uid, entry.packageName)
                                policies = repo.value?.listPolicies() ?: policies
                            }
                        },
                        onToggleLogging = { enabled ->
                            scope.launch {
                                repo.value?.setPolicy(
                                    entry.uid, entry.packageName, entry.policy,
                                    logging = enabled,
                                    notification = entry.notification,
                                )
                                policies = repo.value?.listPolicies() ?: policies
                            }
                        },
                        onToggleNotification = { enabled ->
                            scope.launch {
                                repo.value?.setPolicy(
                                    entry.uid, entry.packageName, entry.policy,
                                    logging = entry.logging,
                                    notification = enabled,
                                )
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

            // ---- Empty state ----
            if (!loading && filtered.isEmpty()) {
                item {
                    AstraCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, null,
                                tint = AstraTeal, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "No policies match \"$searchQuery\"."
                                else
                                    "No su policies yet. Tap + to add one, or " +
                                    "wait for an app to request su.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════
            // REAL TOOL 4: Su request log + stats
            // ══════════════════════════════════════════════════════
            if (logs.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.History, null,
                            tint = AstraTeal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Recent Su Requests (${logs.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                items(items = logs.take(30), key = { "${it.fromUid}-${it.time}" }) { log ->
                    SuLogCard(log)
                }
            }
        }
    }

    // ---- App picker dialog ----
    if (showAppPicker) {
        AppPickerDialog(
            onDismiss = { showAppPicker = false },
            onAppSelected = { packageName, uid ->
                showAppPicker = false
                scope.launch {
                    repo.value?.setPolicy(
                        uid, packageName,
                        MagiskSuRepository.SuPolicy.ASK,
                    )
                    policies = repo.value?.listPolicies() ?: policies
                }
            },
        )
    }
}

// ================================================================
// Su usage stats (aggregated from logs)
// ================================================================

data class SuUsageStats(
    val count: Int,
    val lastAction: String,
    val lastTime: Long,
    val appName: String,
    val todayCount: Int = 0,
    val lastUsed: Long = 0L,
    val recent: List<MagiskSuRepository.SuLogEntry> = emptyList(),
)

object SuStatsAggregator {
    fun aggregate(logs: List<MagiskSuRepository.SuLogEntry>): Map<Int, SuUsageStats> {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis / 1000

        return logs.groupBy { it.fromUid }.mapValues { (_, entries) ->
            val sorted = entries.sortedByDescending { it.time }
            SuUsageStats(
                count = entries.size,
                lastAction = sorted.firstOrNull()?.action ?: "",
                lastTime = sorted.maxOfOrNull { it.time } ?: 0L,
                appName = sorted.firstOrNull()?.appName ?: "",
                todayCount = entries.count { it.time >= startOfToday },
                lastUsed = sorted.firstOrNull()?.time ?: 0L,
                recent = sorted.take(5),
            )
        }
    }

    fun relativeTime(epochSeconds: Long): String {
        if (epochSeconds == 0L) return "never"
        val diff = System.currentTimeMillis() / 1000 - epochSeconds
        return when {
            diff < 60 -> "just now"
            diff < 3600 -> "${diff / 60}m ago"
            diff < 86400 -> "${diff / 3600}h ago"
            else -> "${diff / 86400}d ago"
        }
    }

    fun formatCountdown(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "${h}h${m}m" else "$m:${"%02d".format(s)}"
    }
}

// ================================================================
// App icon helper
// ================================================================

@Composable
private fun rememberAppIcon(packageName: String): android.graphics.Bitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bmp = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888,
            )
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        } catch (_: Exception) {
            null
        }
    }
}

// ================================================================
// App picker dialog — add policy for any installed app
// ================================================================

@Composable
private fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (packageName: String, uid: Int) -> Unit,
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var loadingApps by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val installed = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        apps = installed
            .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { info ->
                AppEntry(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    uid = info.uid,
                )
            }
            .sortedBy { it.label.lowercase() }
        loadingApps = false
    }

    val filtered = remember(search, apps) {
        if (search.isBlank()) apps
        else apps.filter {
            it.label.contains(search, ignoreCase = true) ||
                it.packageName.contains(search, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Su Policy") },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps…") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                )
                Spacer(Modifier.height(8.dp))
                if (loadingApps) {
                    Box(Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAppSelected(app.packageName, app.uid)
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text("${app.packageName} · uid ${app.uid}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AstraOnSurfaceMuted)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

data class AppEntry(
    val packageName: String,
    val label: String,
    val uid: Int,
)

// ================================================================
// SuRiskGrouper — risk-based grouping + scoring
// ================================================================

object SuRiskGrouper {

    data class SuGroup(
        val id: String,
        val label: String,
        val color: Color,
        val icon: ImageVector,
        val entries: List<MagiskSuRepository.SuPolicyEntry>,
    )

    fun attentionScore(entry: MagiskSuRepository.SuPolicyEntry, stats: SuUsageStats?): Int {
        var score = 0
        val now = System.currentTimeMillis() / 1000
        val lastUsed = stats?.lastUsed ?: 0L

        // Recency (0-40)
        score += when {
            lastUsed == 0L -> 0
            now - lastUsed < 3600 -> 40
            now - lastUsed < 86400 -> 30
            now - lastUsed < 7 * 86400 -> 15
            else -> 5
        }

        // Today frequency (0-30)
        score += ((stats?.todayCount ?: 0) * 6).coerceAtMost(30)

        // Policy risk (0-30)
        score += when (entry.policy) {
            MagiskSuRepository.SuPolicy.ALLOW -> if (entry.until == 0L) 30 else 22
            MagiskSuRepository.SuPolicy.ASK -> 12
            MagiskSuRepository.SuPolicy.DENY -> 0
        }

        return score
    }

    fun group(
        policies: List<MagiskSuRepository.SuPolicyEntry>,
        stats: Map<Int, SuUsageStats>,
    ): List<SuGroup> {
        data class Scored(val entry: MagiskSuRepository.SuPolicyEntry, val score: Int)
        val scored = policies.map { Scored(it, attentionScore(it, stats[it.uid])) }

        val high = scored.filter { it.score >= 50 }.sortedByDescending { it.score }
        val normal = scored.filter { it.score in 20..49 }.sortedByDescending { it.score }
        val dormant = scored.filter { it.score < 20 }.sortedByDescending { it.score }

        return listOf(
            SuGroup("high", "Needs Attention", AstraError, Icons.Filled.Warning,
                high.map { it.entry }),
            SuGroup("normal", "Normal", AstraWarning, Icons.Filled.Shield,
                normal.map { it.entry }),
            SuGroup("dormant", "Dormant / Denied", AstraOnSurfaceMuted, Icons.Filled.History,
                dormant.map { it.entry }),
        ).filter { it.entries.isNotEmpty() }
    }
}

// ================================================================
// LockdownBar — panic lockdown button + restore banner
// ================================================================

@Composable
private fun LockdownBar(
    lockdownCount: Int?,
    onLockdown: () -> Unit,
    onRestore: () -> Unit,
    onKeepLocked: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (lockdownCount == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AstraError.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.GppBad, null,
                tint = AstraError, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Lockdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text("Deny all su requests immediately",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted)
            }
            Button(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AstraError),
            ) {
                Icon(Icons.Filled.Lock, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Lock All")
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AstraError.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Warning, null,
                tint = AstraError, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Locked down $lockdownCount apps",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AstraError)
                Text("All su requests are now denied",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted)
            }
            TextButton(onClick = onRestore) {
                Icon(Icons.Filled.Undo, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Restore", color = AstraSuccess, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onKeepLocked) {
                Text("Keep", color = AstraOnSurfaceMuted)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Filled.GppBad, null, tint = AstraError) },
            title = { Text("Lock down all root access?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will immediately set all su policies to Deny:")
                    Text("• Running root shells will fail on next request",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstraOnSurfaceMuted)
                    Text("• Temporary grants will be cleared",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstraOnSurfaceMuted)
                    Text("• You can restore to the pre-lockdown state",
                        style = MaterialTheme.typography.bodySmall,
                        color = AstraOnSurfaceMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; onLockdown() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstraError),
                ) { Text("Confirm Lockdown") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

// ================================================================
// GroupHeader — section header for risk groups
// ================================================================

@Composable
private fun GroupHeader(group: SuRiskGrouper.SuGroup) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier
            .width(4.dp)
            .height(16.dp)
            .background(group.color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Icon(group.icon, null, tint = group.color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(group.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = group.color)
        Spacer(Modifier.width(8.dp))
        Text("(${group.entries.size})",
            style = MaterialTheme.typography.labelMedium,
            color = AstraOnSurfaceMuted)
    }
}

// ================================================================
// Enhanced policy card — countdown, activity dots, expandable details
// ================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuPolicyCard(
    entry: MagiskSuRepository.SuPolicyEntry,
    stats: SuUsageStats?,
    appIcon: android.graphics.Bitmap?,
    onPolicyChange: (MagiskSuRepository.SuPolicy) -> Unit,
    onGrantTemporary: (Int) -> Unit,
    onMakePermanent: () -> Unit,
    onToggleLogging: (Boolean) -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showDurationPicker by remember { mutableStateOf(false) }

    val isTemporary = entry.until > System.currentTimeMillis() / 1000

    val stateColor = when {
        isTemporary -> AstraTeal
        entry.policy == MagiskSuRepository.SuPolicy.ALLOW -> AstraSuccess
        entry.policy == MagiskSuRepository.SuPolicy.ASK -> AstraWarning
        else -> AstraError
    }

    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 14.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ---- Row 1: status bar + icon + name + countdown ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(4.dp).height(42.dp)
                    .background(stateColor, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(12.dp))

                // App icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon.asImageBitmap(),
                            contentDescription = entry.packageName,
                            modifier = Modifier.size(30.dp),
                        )
                    } else {
                        Icon(Icons.Filled.Shield, null,
                            tint = stateColor, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.packageName.substringAfterLast('.'),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1)
                    Text(
                        text = "${entry.packageName} · uid ${entry.uid}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AstraOnSurfaceMuted,
                        maxLines = 1)
                }

                if (isTemporary) {
                    CountdownChip(entry.until)
                }
            }

            // ---- Row 2: activity dots + stats ----
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActivityDots(stats?.recent ?: emptyList())
                Spacer(Modifier.width(10.dp))
                Text(
                    text = buildString {
                        append("today ${stats?.todayCount ?: 0}")
                        append(" · ")
                        append(SuStatsAggregator.relativeTime(stats?.lastUsed ?: 0L))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted)
            }

            // ---- Row 3: segmented policy control (long-press Allow = duration) ----
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                MagiskSuRepository.SuPolicy.entries.forEach { policy ->
                    val selected = entry.policy == policy
                    val color = when (policy) {
                        MagiskSuRepository.SuPolicy.ALLOW ->
                            if (isTemporary) AstraTeal else AstraSuccess
                        MagiskSuRepository.SuPolicy.ASK -> AstraWarning
                        MagiskSuRepository.SuPolicy.DENY -> AstraError
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) color.copy(alpha = 0.22f) else Color.Transparent)
                            .combinedClickable(
                                onClick = {
                                    if (policy == MagiskSuRepository.SuPolicy.ALLOW && isTemporary)
                                        showDurationPicker = true
                                    else if (!selected) onPolicyChange(policy)
                                },
                                onLongClick = {
                                    if (policy == MagiskSuRepository.SuPolicy.ALLOW)
                                        showDurationPicker = true
                                },
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (policy == MagiskSuRepository.SuPolicy.ALLOW && isTemporary)
                                    "Allow ⏱" else policy.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) color else color.copy(alpha = 0.45f),
                        )
                    }
                }
            }
            Text(
                text = "Long-press Allow for timed grant",
                style = MaterialTheme.typography.labelSmall,
                color = AstraOnSurfaceMuted.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp))

            // ---- Row 4: expandable details ----
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (expanded) "Hide" else "Details",
                    style = MaterialTheme.typography.labelMedium,
                    color = AstraOnSurfaceMuted)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ExpandMore, null,
                    tint = AstraOnSurfaceMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (expanded) 180f else 0f))
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.padding(start = 16.dp, top = 4.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Log", style = MaterialTheme.typography.labelSmall,
                                color = AstraOnSurfaceMuted)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = entry.logging,
                                onCheckedChange = onToggleLogging,
                                modifier = Modifier.height(24.dp),
                                colors = SwitchDefaults.colors(checkedTrackColor = AstraTeal),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notify", style = MaterialTheme.typography.labelSmall,
                                color = AstraOnSurfaceMuted)
                            Spacer(Modifier.width(6.dp))
                            Switch(
                                checked = entry.notification,
                                onCheckedChange = onToggleNotification,
                                modifier = Modifier.height(24.dp),
                                colors = SwitchDefaults.colors(checkedTrackColor = AstraTeal),
                            )
                        }
                    }

                    if (!stats?.recent.isNullOrEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, null,
                                tint = AstraOnSurfaceMuted, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Recent requests",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AstraOnSurfaceMuted)
                        }
                        Spacer(Modifier.height(4.dp))
                        stats!!.recent.take(3).forEach { log ->
                            val allowed = log.action.contains("allow", true)
                            Row(Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp)
                                    .background(if (allowed) AstraSuccess else AstraError,
                                        CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Text(log.action,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (allowed) AstraSuccess else AstraError)
                                Spacer(Modifier.width(8.dp))
                                Text(SuStatsAggregator.relativeTime(log.time),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AstraOnSurfaceMuted)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isTemporary) {
                            ActionChip("Make Permanent", AstraSuccess, onMakePermanent)
                        }
                        if (entry.policy != MagiskSuRepository.SuPolicy.DENY) {
                            ActionChip("Revoke Now", AstraWarning,
                                { onPolicyChange(MagiskSuRepository.SuPolicy.DENY) })
                        }
                        ActionChip("Delete", AstraError, onDelete, outline = true)
                    }
                }
            }
        }
    }

    if (showDurationPicker) {
        DurationPickerDialog(
            onDismiss = { showDurationPicker = false },
            onSelect = { minutes ->
                showDurationPicker = false
                if (minutes == null) onMakePermanent()
                else onGrantTemporary(minutes)
            },
        )
    }
}

@Composable
private fun CountdownChip(untilEpochSeconds: Long) {
    var remaining by remember(untilEpochSeconds) {
        mutableStateOf((untilEpochSeconds - System.currentTimeMillis() / 1000)
            .coerceAtLeast(0))
    }
    LaunchedEffect(untilEpochSeconds) {
        while (true) {
            remaining = (untilEpochSeconds - System.currentTimeMillis() / 1000)
                .coerceAtLeast(0)
            if (remaining <= 0) break
            delay(1000)
        }
    }
    if (remaining > 0) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(AstraTeal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Filled.Timer, null, tint = AstraTeal,
                modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(SuStatsAggregator.formatCountdown(remaining),
                color = AstraTeal, fontFamily = FontFamily.Monospace,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActivityDots(recent: List<MagiskSuRepository.SuLogEntry>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val dots = recent.takeLast(5)
        dots.forEach { log ->
            val allowed = log.action.contains("allow", true)
            Box(Modifier.size(7.dp)
                .background(if (allowed) AstraSuccess else AstraError, CircleShape))
        }
        repeat((5 - dots.size).coerceAtLeast(0)) {
            Box(Modifier.size(7.dp)
                .background(AstraOnSurfaceMuted.copy(alpha = 0.25f), CircleShape))
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    color: Color,
    onClick: () -> Unit,
    outline: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (outline) Color.Transparent else color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun DurationPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow Root Access", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Choose duration (auto-revokes on expiry):",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "5 minutes" to 5,
                    "15 minutes" to 15,
                    "1 hour" to 60,
                    "8 hours" to 480,
                    "Permanent" to null,
                ).forEach { (label, minutes) ->
                    TextButton(
                        onClick = { onSelect(minutes) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ================================================================
// ADB Shell special entry (uid 2000)
// ================================================================

@Composable
private fun AdbShellPolicyCard(
    policies: List<MagiskSuRepository.SuPolicyEntry>,
    onPolicyChange: (MagiskSuRepository.SuPolicy) -> Unit,
) {
    val adbEntry = policies.find { it.uid == AdbManager.SHELL_UID }
    val currentPolicy = adbEntry?.policy ?: MagiskSuRepository.SuPolicy.ASK

    val (statusIcon, statusColor, statusLabel) = when (currentPolicy) {
        MagiskSuRepository.SuPolicy.ALLOW ->
            Triple(Icons.Filled.CheckCircle, AstraSuccess, "Allowed")
        MagiskSuRepository.SuPolicy.ASK ->
            Triple(Icons.Filled.HelpOutline, AstraWarning, "Ask")
        MagiskSuRepository.SuPolicy.DENY ->
            Triple(Icons.Filled.Block, AstraError, "Denied")
    }

    AstraCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(statusIcon, null, tint = statusColor,
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("ADB Shell",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text("com.android.shell · uid ${AdbManager.SHELL_UID} · $statusLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MagiskSuRepository.SuPolicy.entries.forEach { policy ->
                val selected = currentPolicy == policy
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
                    Text(policy.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold
                                     else FontWeight.Normal,
                        color = if (selected) color
                                else color.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Controls whether 'adb shell su' is granted, denied, " +
                "or prompts. Applies to uid 2000 (shell user).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ================================================================
// No-root info card
// ================================================================

@Composable
private fun NoRootInfoCard() {
    AstraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, null,
                tint = AstraWarning, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Root Backend Required",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "This page manages su access policies. It reads and " +
                "writes your root backend's policy database directly — " +
                "changes take effect immediately.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Text("Once a root backend is installed, you can:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        listOf(
            "View all apps that have requested su access",
            "Grant, deny, or set \"ask\" per app",
            "See recent su request history with timestamps",
            "Control logging and notifications per app",
            "Run commands in the embedded ADB console",
            "Open the full superuser terminal",
        ).forEach { feature ->
            Row(modifier = Modifier.padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top) {
                Text("•", color = AstraWarning,
                    modifier = Modifier.padding(end = 8.dp))
                Text(feature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Supported root backends:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        listOf(
            "Magisk" to "Most common. Patches boot image.",
            "KernelSU" to "Kernel-level. GKI compatible.",
            "APatch" to "Kernel patch. No boot modification.",
            "AstraRoot" to "AstraVeil's own backend (coming soon).",
        ).forEach { (name, desc) ->
            Row(modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top) {
                Text(name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(80.dp))
                Text(desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted)
            }
        }
    }
}

// ================================================================
// Su log card
// ================================================================

@Composable
private fun SuLogCard(log: MagiskSuRepository.SuLogEntry) {
    val sdf = remember {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    }
    val allowed = log.action.contains("allow", ignoreCase = true)

    AstraCard(contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (allowed) Icons.Filled.CheckCircle else Icons.Filled.Block,
                null,
                tint = if (allowed) AstraSuccess else AstraError,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = log.appName.ifBlank { log.packageName },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
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
