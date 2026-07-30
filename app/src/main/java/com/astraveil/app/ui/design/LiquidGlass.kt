package com.astraveil.app.ui.design

import android.os.Build
import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass design tokens.
 *
 * Multi-layer optical model (bottom → top):
 *   Layer 0  base tint          6-13% white
 *   Layer 1  vertical gradient   top 13% → bottom 2%  (overhead light)
 *   Layer 2  specular highlight  radial, follows touch
 *   Layer 3  border glow         1.5dp inner stroke
 *
 * No Modifier.blur() on the surface itself — that blurs the glass's
 * OWN content, not the background. Real background blur requires
 * window-level FLAG_BLUR_BEHIND (API 31+) which is applied at the
 * Activity level, not per-card.
 */
object LiquidGlass {

    // ---- Layer 0: base ----
    val BaseTint: Color = Color.White.copy(alpha = 0.07f)
    val PressedTint: Color = Color.White.copy(alpha = 0.13f)

    // ---- Layer 1: gradient ----
    val GradientTop: Color = Color.White.copy(alpha = 0.13f)
    val GradientBottom: Color = Color.White.copy(alpha = 0.02f)

    // ---- Layer 2: specular ----
    val SpecularCore: Color = Color.White.copy(alpha = 0.22f)
    val SpecularPressed: Color = Color.White.copy(alpha = 0.38f)
    val SpecularEdge: Color = Color.White.copy(alpha = 0.0f)

    // ---- Layer 3: border ----
    val BorderGlow: Color = Color.White.copy(alpha = 0.20f)
    val BorderOuter: Color = Color.White.copy(alpha = 0.09f)

    // ---- Accents ----
    val AccentViolet: Color = Color(0xFF8B5CF6).copy(alpha = 0.10f)
    val AccentTeal: Color = Color(0xFF14B8A6).copy(alpha = 0.08f)
    val AccentError: Color = Color(0xFFEF4444).copy(alpha = 0.08f)
    val AccentAmber: Color = Color(0xFFF59E0B).copy(alpha = 0.08f)

    // ---- Nav bar ----
    val NavSelected: Color = Color.White.copy(alpha = 0.16f)
    val NavIconActive: Color = Color(0xFFA78BFA)
    val NavIconInactive: Color = Color.White.copy(alpha = 0.45f)

    // ---- Animation ----
    const val PressScale = 0.965f
    const val SpringStiffness = 400f
    const val SpringDamping = 0.72f

    // ---- Capability ----
    val supportsWindowBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
