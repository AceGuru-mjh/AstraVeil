package com.astraveil.app.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted

private const val PREFS_NAME = "astra_ui_prefs"

private data class NotifChannel(
    val key: String,
    val title: String,
    val subtitle: String,
    val default: Boolean,
)

private val channels = listOf(
    NotifChannel("notif_su_requests", "Superuser Requests", "Prompt when an app requests root access", true),
    NotifChannel("notif_updates", "Updates", "New AstraVeil or module releases", true),
    NotifChannel("notif_modules", "Modules", "Module install / start / stop / failure", true),
    NotifChannel("notif_daemon", "Daemon", "AstraDaemon connection state changes", true),
    NotifChannel("notif_security", "Security", "Permission denials, policy refusals, audit alerts", true),
)

/**
 * Notification settings — per-channel toggles backed by
 * `SharedPreferences("astra_ui_prefs")`. AstraNotificationManager is
 * expected to consult these keys before posting to each channel.
 */
@Composable
fun NotificationsScreen() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Choose which channels can post notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Notification Channels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Each toggle maps to an Android notification channel. Disabling a channel mutes its notifications system-wide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
                Spacer(Modifier.height(10.dp))
                channels.forEach { ch ->
                    val checked = remember(ch.key) {
                        mutableStateOf(prefs.getBoolean(ch.key, ch.default))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ch.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = ch.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = AstraOnSurfaceMuted,
                            )
                        }
                        Switch(
                            checked = checked.value,
                            onCheckedChange = { value ->
                                checked.value = value
                                prefs.edit().putBoolean(ch.key, value).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AstraAccent,
                                checkedTrackColor = AstraAccent.copy(alpha = 0.35f),
                            ),
                        )
                    }
                }
            }
        }
    }
}
