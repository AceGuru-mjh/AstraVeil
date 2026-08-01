package com.astraveil.app.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Canonical corner-radius scale for all AstraVeil surfaces.
 *
 * The single source of truth for corner radii across the app. Every
 * container — [AstraSurface], the deprecated [GlassCard] /
 * [LiquidGlassCard] shims, the navigation bar, dialogs — should derive
 * its corner radius from these tokens so the visual rhythm stays
 * consistent.
 *
 * Values are [Dp] (not [RoundedCornerShape]) so callers can pass them
 * to [AstraSurface.cornerRadius] or wrap them in [RoundedCornerShape]
 * when a Shape is required.
 */
object AstraShapes {
    val sm = 10.dp
    val md = 16.dp
    val lg = 22.dp
    val xl = 28.dp
    val pill = 999.dp
}

/**
 * Legacy corner-radius tokens. Preserved as a thin alias over [AstraShapes]
 * so existing call sites keep compiling while new code adopts [AstraShapes]
 * directly.
 *
 * The property names (`Small` / `Medium` / …) and the
 * [RoundedCornerShape] return type are kept for source compatibility;
 * the underlying radii now resolve through [AstraShapes].
 */
@Deprecated("Use AstraShapes", ReplaceWith("AstraShapes"))
object GlassShapes {
    val Small: RoundedCornerShape get() = RoundedCornerShape(AstraShapes.sm)
    val Medium: RoundedCornerShape get() = RoundedCornerShape(AstraShapes.md)
    val Large: RoundedCornerShape get() = RoundedCornerShape(AstraShapes.lg)
    val ExtraLarge: RoundedCornerShape get() = RoundedCornerShape(AstraShapes.xl)
    val Pill: RoundedCornerShape get() = RoundedCornerShape(AstraShapes.pill)
}
