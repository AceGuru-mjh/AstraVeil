package com.astraveil.app.ui.design

import androidx.compose.ui.graphics.Color

/**
 * Astra Glass Design System — color tokens.
 *
 * The glass aesthetic uses translucent white surfaces over a deep dark
 * background, with a soft glow accent. No opaque card fills — every
 * surface is a frosted layer that lets the ambient background show
 * through.
 */
object AstraGlass {
    /** Translucent white surface fill (8% opacity). */
    val Surface = Color.White.copy(alpha = 0.08f)

    /** Translucent white border (16% opacity). */
    val Border = Color.White.copy(alpha = 0.16f)

    /** Accent glow — soft blue-violet for highlights. */
    val Glow = Color(0xFF8B5CF6)

    /** Secondary accent — teal for success states. */
    val Teal = Color(0xFF2DD4BF)

    /** Deep background base. */
    val Background = Color(0xFF070809)

    /** Elevated surface (slightly more opaque than [Surface]). */
    val SurfaceElevated = Color.White.copy(alpha = 0.12f)
}
