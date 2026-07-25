package com.astraveil.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * AstraVeil Material3 theme.
 *
 * Dark is the canonical aesthetic — the dark scheme is fully fleshed out
 * with the violet/teal accent palette. Light is provided for accessibility
 * but is intentionally less elaborate.
 *
 * @param darkTheme   force dark/light; defaults to the system setting.
 * @param dynamicColor Material You dynamic colours — disabled by default
 *                     because the AstraVeil brand palette is intentional.
 */
@Composable
fun AstraVeilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            // Dynamic colour intentionally not wired (Android 12+ only and
            // would override the AstraVeil brand). Fall through to the brand
            // palette below.
            if (darkTheme) AstraDarkColorScheme else AstraLightColorScheme
        }
        darkTheme -> AstraDarkColorScheme
        else -> AstraLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            // Light icons on the deep dark background regardless of scheme.
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AstraTypography,
        content = content
    )
}

/** Deep-dark violet/teal palette — the canonical AstraVeil aesthetic. */
val AstraDarkColorScheme = darkColorScheme(
    primary = AstraAccent,
    onPrimary = AstraAccentOn,
    primaryContainer = AstraAccentDim,
    onPrimaryContainer = Color(0xFFE9DEFB),
    secondary = AstraTeal,
    onSecondary = Color(0xFF062B26),
    secondaryContainer = AstraTealDim,
    onSecondaryContainer = Color(0xFFCFFAF2),
    tertiary = Color(0xFFD8B4FE),
    onTertiary = Color(0xFF1F0F2E),
    tertiaryContainer = Color(0xFF3A2657),
    onTertiaryContainer = Color(0xFFEEDBFF),
    error = AstraError,
    onError = Color(0xFF1F0808),
    errorContainer = AstraErrorDim,
    onErrorContainer = Color(0xFFFFD6D6),
    background = AstraBackground,
    onBackground = AstraOnBackground,
    surface = AstraSurface,
    onSurface = AstraOnSurface,
    surfaceVariant = AstraSurfaceVariant,
    onSurfaceVariant = AstraOnSurfaceMuted,
    surfaceTint = AstraAccent,
    outline = AstraOutline,
    outlineVariant = AstraOutlineVariant,
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE5E7EB),
    inverseOnSurface = Color(0xFF14161D),
    inversePrimary = AstraAccent
)

/** Light scheme — used only when the user forces light mode. */
val AstraLightColorScheme = lightColorScheme(
    primary = AstraAccentLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DEFB),
    onPrimaryContainer = Color(0xFF1F0F2E),
    secondary = AstraTealLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFFAF2),
    onSecondaryContainer = Color(0xFF062B26),
    tertiary = Color(0xFF6D28D9),
    onTertiary = Color(0xFFFFFFFF),
    background = AstraLightBackground,
    onBackground = AstraLightOnBackground,
    surface = AstraLightSurface,
    onSurface = AstraLightOnSurface,
    surfaceVariant = AstraLightSurfaceVariant,
    onSurfaceVariant = AstraLightOnSurfaceMuted,
    outline = AstraLightOutline,
    outlineVariant = Color(0xFFC9CED9)
)
