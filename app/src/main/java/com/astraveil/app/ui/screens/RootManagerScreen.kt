package com.astraveil.app.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.AstraVeilApplication
import com.astraveil.app.ui.design.AstraGlass
import com.astraveil.app.ui.design.LiquidGlass
import com.astraveil.app.ui.design.LiquidGlassCard
import com.astraveil.app.viewmodel.StatusViewModel

/**
 * A real app entry queried from PackageManager.
 */
data class SuAppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    var granted: Boolean = false,
)

/**
 * Superuser management screen — shows REAL installed applications
 * from PackageManager and tracks su grants via AstraCore's
 * PermissionEngine (capability = "su", moduleId = packageName).
 *
 * Phase 0: grants are stored in PermissionEngine (persisted to
 * astra_config.json via AstraCore.requestAndPersistPermission).
 * Phase 1+: grants will be enforced by astrad's su daemon.
 */
@Composable
fun RootManagerScreen(viewModel: StatusViewModel) {
    val context = LocalContext.current
    val pm = context.packageManager
    val permissionEngine = AstraVeilApplication.core.permissionEngine

    var searchQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    val appEntries = remember { mutableStateListOf<SuAppEntry>() }

    // Load real app list once
    LaunchedEffect(Unit) {
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val entries = installed
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { info ->
                SuAppEntry(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = try { pm.getApplicationIcon(info) } catch (_: Exception) { null },
                    granted = permissionEngine.canExecute(info.packageName, "su"),
                )
            }
            .sortedBy { it.label.lowercase() }

        appEntries.clear()
        appEntries.addAll(entries)
        loading = false
    }

    val filtered = remember(searchQuery, appEntries.size) {
        if (searchQuery.isBlank()) appEntries.toList()
        else appEntries.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val grantedCount = appEntries.count { it.granted }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
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
                    text = "Manage which apps can execute commands as root.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // ---- Status card ----
        item {
            LiquidGlassCard(accent = LiquidGlass.AccentViolet) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Shield,
                        null,
                        tint = AstraGlass.Glow,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "$grantedCount app(s) granted su",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${appEntries.size} user apps installed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---- Search ----
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
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

        // ---- App list ----
        items(
            items = filtered,
            key = { it.packageName },
        ) { entry ->
            SuAppCard(
                entry = entry,
                onToggle = { granted ->
                    entry.granted = granted
                    if (granted) {
                        permissionEngine.grant(entry.packageName, "su")
                    } else {
                        permissionEngine.revoke(entry.packageName, "su")
                    }
                },
            )
        }

        // ---- Empty ----
        if (!loading && filtered.isEmpty()) {
            item {
                LiquidGlassCard {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No apps match \"$searchQuery\""
                               else "No user apps installed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuAppCard(
    entry: SuAppEntry,
    onToggle: (Boolean) -> Unit,
) {
    LiquidGlassCard(
        enablePressEffect = false,
        contentPadding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        AstraGlass.SurfaceElevated,
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (entry.icon != null) {
                    AppIcon(drawable = entry.icon, modifier = Modifier.size(32.dp))
                } else {
                    Icon(
                        Icons.Filled.Shield,
                        null,
                        tint = AstraGlass.Glow,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Name + package
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Su toggle
            Switch(
                checked = entry.granted,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = AstraGlass.Glow,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = AstraGlass.Surface,
                ),
            )
        }
    }
}

/**
 * Render a Drawable as a Compose Image without Accompanist.
 * Converts to Bitmap once and caches via remember.
 */
@Composable
private fun AppIcon(drawable: Drawable, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
}
