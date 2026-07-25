package com.astraveil.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * AstraVeil colour palette.
 *
 * The aesthetic is a deep near-black surface with violet / teal accents —
 * intentionally NOT indigo or blue. These raw colors are consumed by
 * [AstraVeilTheme] to build Material3 dark/light color schemes.
 */

// ---- Core surfaces (deep dark first) ----
val AstraBackground = Color(0xFF0B0D12)
val AstraSurface = Color(0xFF13161D)
val AstraSurfaceVariant = Color(0xFF1B1F28)
val AstraSurfaceElevated = Color(0xFF20242F)
val AstraOutline = Color(0xFF2A2F3C)
val AstraOutlineVariant = Color(0xFF3A4050)

// ---- Accents ----
val AstraAccent = Color(0xFF8B5CF6)         // violet — primary
val AstraAccentOn = Color(0xFF1A0F2E)       // text on violet
val AstraAccentDim = Color(0xFF5B3FA3)      // muted violet
val AstraTeal = Color(0xFF2DD4BF)           // teal — secondary
val AstraTealDim = Color(0xFF0F5A52)

// ---- Status semantics ----
val AstraSuccess = Color(0xFF34D399)
val AstraSuccessDim = Color(0xFF0B3D2E)
val AstraWarning = Color(0xFFFBBF24)
val AstraWarningDim = Color(0xFF3D2F08)
val AstraError = Color(0xFFF87171)
val AstraErrorDim = Color(0xFF3D1212)
val AstraInfo = Color(0xFF60A5FA)           // used sparingly — never as primary

// ---- Text ----
val AstraOnBackground = Color(0xFFE5E7EB)
val AstraOnSurface = Color(0xFFD1D5DB)
val AstraOnSurfaceMuted = Color(0xFF8B90A0)
val AstraOnSurfaceFaint = Color(0xFF5C6276)

// ---- Light scheme counterparts (secondary, used only if forced to light) ----
val AstraLightBackground = Color(0xFFF4F5F8)
val AstraLightSurface = Color(0xFFFFFFFF)
val AstraLightSurfaceVariant = Color(0xFFEDEFF4)
val AstraLightOutline = Color(0xFFC9CED9)
val AstraLightOnBackground = Color(0xFF14161D)
val AstraLightOnSurface = Color(0xFF2A2E3A)
val AstraLightOnSurfaceMuted = Color(0xFF5C6276)
val AstraAccentLight = Color(0xFF6D28D9)
val AstraTealLight = Color(0xFF0D9488)
