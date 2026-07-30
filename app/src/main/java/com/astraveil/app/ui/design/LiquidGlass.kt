package com.astraveil.app.ui.design

import android.os.Build
import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass tokens — adapted from liquid-glass-react.
 *
 * Reference: https://github.com/rdev/liquid-glass-react
 *
 * The web library uses SVG feDisplacementMap to refract the background.
 * On Android we simulate refraction with edge gradient lenses +
 * chromatic aberration strokes + specular highlights.
 */
object LiquidGlass {

    // ---- Surface layers ----
    /** Base tint. liquid-glass-react uses ~6% white backdrop. */
    val BaseTint: Color = Color.White.copy(alpha = 0.06f)
    val PressedTint: Color = Color.White.copy(alpha = 0.12f)

    /** Vertical gradient: overhead light simulation. */
    val GradientTop: Color = Color.White.copy(alpha = 0.14f)
    val GradientBottom: Color = Color.White.copy(alpha = 0.02f)

    // ---- Edge refraction (simulates feDisplacementMap) ----
    /** Maximum alpha at the very edge of the glass. */
    const val EdgeRefractionAlpha = 0.28f
    /** Width of the refraction zone in dp. */
    const val EdgeRefractionWidthDp = 18f
    /** Number of concentric strokes for smooth falloff. */
    const val EdgeRefractionSteps = 12

    // ---- Chromatic aberration (simulates RGB channel split) ----
    val AberrationRed: Color = Color.Red.copy(alpha = 0.06f)
    val AberrationBlue: Color = Color.Blue.copy(alpha = 0.06f)
    /** Pixel offset for RGB split. */
    const val AberrationOffsetPx = 1.5f

    // ---- Specular highlight ----
    val SpecularCore: Color = Color.White.copy(alpha = 0.30f)
    val SpecularPressed: Color = Color.White.copy(alpha = 0.45f)
    val SpecularEdge: Color = Color.White.copy(alpha = 0.0f)

    // ---- Border ----
    val BorderInner: Color = Color.White.copy(alpha = 0.30f)
    val BorderOuter: Color = Color.White.copy(alpha = 0.12f)
    /** Second border layer (overlay blend simulation). */
    val BorderOverlay: Color = Color.White.copy(alpha = 0.08f)

    // ---- Accents ----
    val AccentViolet: Color = Color(0xFF8B5CF6).copy(alpha = 0.10f)
    val AccentTeal: Color = Color(0xFF14B8A6).copy(alpha = 0.08f)
    val AccentError: Color = Color(0xFFEF4444).copy(alpha = 0.08f)
    val AccentAmber: Color = Color(0xFFF59E0B).copy(alpha = 0.08f)

    // ---- Nav bar ----
    val NavSelected: Color = Color.White.copy(alpha = 0.20f)
    val NavIconActive: Color = Color(0xFFA78BFA)
    val NavIconInactive: Color = Color.White.copy(alpha = 0.50f)
    val NavLabelActive: Color = Color.White.copy(alpha = 0.95f)
    val NavLabelInactive: Color = Color.White.copy(alpha = 0.38f)

    // ---- Elasticity (matches liquid-glass-react defaults) ----
    /** Press scale. liquid-glass-react uses 0.96 on click. */
    const val PressScale = 0.96f
    /** Spring stiffness for release bounce. */
    const val SpringStiffness = 300f
    /**
     * Damping ratio. < 1.0 gives overshoot (liquid wobble).
     * liquid-glass-react elasticity=0.15 → similar feel at 0.65.
     */
    const val SpringDamping = 0.65f

    // ---- Shadow ----
    const val ShadowElevationDp = 12f
    const val ShadowPressedElevationDp = 4f

    // ---- Capability ----
    val supportsWindowBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
