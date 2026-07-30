package com.astraveil.app.ui.design

import android.os.Build
import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass design tokens.
 *
 * Extends [AstraGlass] with multi-layer optical properties inspired by
 * Apple's Liquid Glass (iOS 26). All tokens are API-level agnostic;
 * the rendering components decide which effects to enable at runtime.
 */
object LiquidGlass {

    // ---- Base layers (bottom → top) ----

    /** Layer 0: deep tint. Slightly stronger than AstraGlass.Surface. */
    val BaseTint: Color = Color.White.copy(alpha = 0.06f)

    /** Layer 1: vertical gradient top. Catches "overhead light". */
    val GradientTop: Color = Color.White.copy(alpha = 0.14f)

    /** Layer 1: vertical gradient bottom. Falls into shadow. */
    val GradientBottom: Color = Color.White.copy(alpha = 0.03f)

    /** Layer 2: specular highlight core (follows touch). */
    val SpecularCore: Color = Color.White.copy(alpha = 0.28f)

    /** Layer 2: specular highlight falloff. */
    val SpecularEdge: Color = Color.White.copy(alpha = 0.0f)

    /** Layer 3: inner border glow. */
    val BorderGlow: Color = Color.White.copy(alpha = 0.22f)

    /** Layer 3: outer border (subtle). */
    val BorderOuter: Color = Color.White.copy(alpha = 0.10f)

    // ---- Interaction states ----

    /** Pressed: base tint increases (glass "compresses"). */
    val PressedTint: Color = Color.White.copy(alpha = 0.12f)

    /** Pressed: specular intensifies. */
    val PressedSpecular: Color = Color.White.copy(alpha = 0.40f)

    // ---- Accent variants ----

    /** Violet accent glass (for primary actions). */
    val AccentViolet: Color = AstraGlass.Glow.copy(alpha = 0.10f)

    /** Teal accent glass (for success / capability). */
    val AccentTeal: Color = AstraGlass.Teal.copy(alpha = 0.08f)

    /** Error accent glass. */
    val AccentError: Color = Color(0xFFF87171).copy(alpha = 0.08f)

    // ---- Blur ----

    /** Background blur radius (API 31+ only). */
    const val BlurRadiusDp = 24f

    /** Whether real background blur is available. */
    val supportsBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Whether AGSL shaders are available. */
    val supportsAgsl: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    // ---- Animation ----

    /** Press scale factor. */
    const val PressScale = 0.965f

    /** Spring stiffness for release bounce. */
    const val SpringStiffness = 400f

    /** Spring damping ratio. */
    const val SpringDamping = 0.72f

    /** Specular follow animation duration (ms). */
    const val SpecularFollowMs = 180

    /** Parallax factor for scroll depth. */
    const val ParallaxFactor = 0.04f
}
