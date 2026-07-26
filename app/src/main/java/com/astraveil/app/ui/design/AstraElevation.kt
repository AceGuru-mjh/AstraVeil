package com.astraveil.app.ui.design

/**
 * Elevation tokens for the glass system.
 *
 * Glass does not use shadows the way Material3 does — instead, elevation
 * is conveyed by increasing surface opacity. A higher [GlassElevation]
 * means a more opaque (more "lifted") surface.
 */
enum class GlassElevation(val surfaceAlpha: Float) {
    Level0(0.04f),   // background blend
    Level1(0.08f),   // default card
    Level2(0.12f),   // elevated card / dialog
    Level3(0.16f),   // top sheet
}
