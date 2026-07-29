package com.astraveil.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted

/**
 * Placeholder screen for settings sub-pages that are not yet
 * implemented (General, Security, Provider-settings, Modules-settings,
 * Daemon, Developer).
 *
 * Previously, tapping any of these entries in [SettingsScreen] called
 * `navController.navigate(route)` for a route that was NOT registered in
 * the NavHost, which caused Navigation Compose to throw
 * IllegalArgumentException and crash the app. This screen is the safe
 * target for every settings sub-route so the tap never crashes.
 *
 * Each sub-route is wired in AstraVeilApp.kt to an instance of this
 * composable with the matching title.
 */
@Composable
fun ComingSoonScreen(
    title: String,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .background(
                    color = AstraAccent.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = AstraAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = null,
                    tint = AstraAccent,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "此功能正在开发中，敬请期待。\nThis feature is under development.",
                style = MaterialTheme.typography.bodySmall,
                color = AstraOnSurfaceMuted,
            )
        }
    }
}
