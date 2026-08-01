package com.astraveil.app.ui.screens.settings

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.ThemeMode
import com.astraveil.app.ui.AstraStrings
import java.util.Locale

private const val PREFS_NAME = "astra_ui_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_LANGUAGE_CODE = "language_code"

/**
 * Preferences — merged Appearance + Language page (P2-16 consolidation).
 *
 * Both settings persist to `SharedPreferences("astra_ui_prefs")` and apply
 * immediately by calling `Activity.recreate()`:
 *  - Theme: [MainActivity] reads `theme_mode` at startup and passes it to
 *    [com.astraveil.app.ui.theme.AstraVeilTheme], so recreate() picks up
 *    the new color scheme.
 *  - Language: [AstraStrings.setLocaleOverride] is called before recreate()
 *    so the new locale takes effect on rebuild.
 */
@Composable
fun PreferencesScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var themeMode by remember {
        mutableStateOf(ThemeMode.fromString(prefs.getString(KEY_THEME_MODE, "dark")))
    }
    var language by remember {
        mutableStateOf(
            prefs.getString(KEY_LANGUAGE_CODE, Locale.getDefault().language) ?: "en"
        )
    }

    fun applyTheme(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.toPrefString()).apply()
        (context as? Activity)?.recreate()
    }

    fun applyLanguage(code: String) {
        language = code
        AstraStrings.setLocaleOverride(Locale(code))
        prefs.edit().putString(KEY_LANGUAGE_CODE, code).apply()
        (context as? Activity)?.recreate()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Preferences",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
        }

        // ── 主题 ──
        item {
            AstraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DarkMode, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.padding(top = 8.dp))

                listOf(
                    ThemeMode.DARK to "Dark (recommended)",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.SYSTEM to "Follow system",
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyTheme(mode) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = themeMode == mode,
                            onClick = { applyTheme(mode) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        if (themeMode == mode) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.Check, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Text("AstraVeil is designed dark-first. Light theme is " +
                    "fully functional but less tested.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted)
            }
        }

        // ── 语言 ──
        item {
            AstraCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Language, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Language",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.padding(top = 8.dp))

                listOf("en" to "English", "zh" to "中文").forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyLanguage(code) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = language == code,
                            onClick = { applyLanguage(code) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        if (language == code) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.Check, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Text("Other languages will be added in future releases.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted)
            }
        }
    }
}
