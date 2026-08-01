package com.astraveil.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.ui.AstraStrings
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.theme.AstraAccent

data class SettingEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val route: String,
)

object SettingEntries {
    val Security = SettingEntry(Icons.Filled.Security, "Security", "Permission mode, trusted keys, audit", "settings_security")
    val Provider = SettingEntry(Icons.Filled.Power, "Provider", "Root backend selection", "settings_provider")
    val Modules = SettingEntry(Icons.Filled.Extension, "Modules", "Storage, trust, auto-start", "settings_modules")
    val Daemon = SettingEntry(Icons.Filled.Devices, "Daemon", "Service status, enable toggle", "settings_daemon")
    val Notifications = SettingEntry(Icons.Filled.Notifications, "Notifications", "Channel toggles", "settings_notifications")
    val Preferences = SettingEntry(Icons.Filled.Palette, "Preferences", "Theme and language", "settings_preferences")
    val UpdateBackup = SettingEntry(Icons.Filled.SystemUpdate, "Update & Backup", "Updates, export/import data", "settings_update_backup")
    val Diagnostics = SettingEntry(Icons.Filled.BugReport, "Diagnostics", "System health report, capabilities, warnings", "settings_diagnostics")
    val Developer = SettingEntry(Icons.Filled.BugReport, "Developer", "Version, log level, diagnostic export", "settings_developer")
    val About = SettingEntry(Icons.Filled.Info, "About", "Version, architecture, license", "settings_about")

    val list = listOf(Security, Provider, Modules, Daemon, Notifications, Preferences, UpdateBackup, Diagnostics, Developer, About)
}

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(AstraStrings.settingsTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(AstraStrings.settingsSubtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(SettingEntries.list) { entry ->
            AstraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(entry.route) }
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(entry.icon, null, tint = AstraAccent, modifier = Modifier.padding(end = 14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(entry.subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
