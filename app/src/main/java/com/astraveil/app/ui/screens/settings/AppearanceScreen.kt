package com.astraveil.app.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
private const val KEY_THEME_MODE = "theme_mode"

private data class ThemeOption(val key: String, val label: String, val hint: String)

private val themeOptions = listOf(
    ThemeOption("light", "Light", "Always use the light color scheme"),
    ThemeOption("dark", "Dark", "Always use the dark color scheme (canonical AstraVeil)"),
    ThemeOption("system", "System default", "Follow the system dark-mode setting"),
)

/**
 * Appearance settings — theme mode (light / dark / system).
 *
 * Persists the user's choice to `SharedPreferences("astra_ui_prefs")` under
 * [KEY_THEME_MODE]. `Theme.kt` currently derives dark/light purely from the
 * system configuration; once it learns to read this preference, the change
 * will apply on the next recomposition. Until then, the screen notes that
 * the choice applies on restart.
 */
@Composable
fun AppearanceScreen() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var selected by remember {
        mutableStateOf(prefs.getString(KEY_THEME_MODE, "system") ?: "system")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Theme, color scheme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Applies on next app restart.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                themeOptions.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == opt.key,
                            onClick = {
                                selected = opt.key
                                prefs.edit().putString(KEY_THEME_MODE, opt.key).apply()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AstraAccent,
                            ),
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = opt.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = opt.hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = AstraOnSurfaceMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
