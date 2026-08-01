package com.astraveil.app.ui.screens.settings

import android.app.Activity
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
import com.astraveil.app.ui.AstraStrings
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import java.util.Locale

private const val PREFS_NAME = "astra_ui_prefs"
private const val KEY_LANGUAGE_CODE = "language_code"

private data class LanguageOption(val code: String, val nativeName: String, val englishName: String)

private val languages = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("zh", "中文", "Chinese"),
    LanguageOption("ja", "日本語", "Japanese"),
    LanguageOption("ko", "한국어", "Korean"),
    LanguageOption("es", "Español", "Spanish"),
    LanguageOption("de", "Deutsch", "German"),
    LanguageOption("fr", "Français", "French"),
    LanguageOption("pt", "Português", "Portuguese"),
    LanguageOption("ru", "Русский", "Russian"),
)

/**
 * Language settings.
 *
 * On select:
 *  1. Persists the chosen language code to SharedPreferences.
 *  2. Calls [AstraStrings.setLocaleOverride] so subsequent string reads
 *     pick up the new locale.
 *  3. Calls `Activity.recreate()` so the in-flight Compose tree re-builds
 *     with the new locale (the simplest reliable way to force a full
 *     recomposition without per-call re-issuing).
 *
 * Note: AstraStrings currently only translates zh vs. everything-else, so
 * only English / Chinese will see a visible string change. The full
 * language list is offered anyway so the user's choice is recorded for
 * when richer translations land.
 */
@Composable
fun LanguageScreen() {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var selected by remember {
        mutableStateOf(prefs.getString(KEY_LANGUAGE_CODE, Locale.getDefault().language) ?: "en")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Language",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "App display language",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AstraCard(tier = SurfaceTier.CONTENT) {
                Text(
                    text = "Display Language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Note: only English and 中文 are fully translated today; other selections are saved for future use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == lang.code,
                            onClick = {
                                selected = lang.code
                                prefs.edit().putString(KEY_LANGUAGE_CODE, lang.code).apply()
                                AstraStrings.setLocaleOverride(Locale(lang.code))
                                (context as? Activity)?.recreate()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AstraAccent,
                            ),
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = lang.nativeName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = lang.englishName,
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
