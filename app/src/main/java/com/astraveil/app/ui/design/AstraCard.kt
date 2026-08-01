package com.astraveil.app.ui.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Surface tiers — the SINGLE source of truth for container treatment.
 *
 * The tier carries MEANING, not just style:
 *
 *  [CONTENT]  Quiet, readable. Default for lists, settings, info rows,
 *             policy cards, diagnostics. The workhorse.
 *
 *  [ELEVATED] Slight emphasis for section grouping or key summaries
 *             that should sit above the content layer.
 *
 *  [LIQUID]   ALIVE. Reserved for live state — a running module, a
 *             connected daemon, an in-progress download, a live
 *             terminal. Renders as an ELEVATED card with an accent
 *             tint; the liquid glass effect has been removed in favor
 *             of pure Material 3 (cleaner, better dark-mode support,
 *             better performance). The semantic distinction is
 *             preserved so callers can still declare "this is live".
 */
enum class SurfaceTier {
    CONTENT,
    ELEVATED,
    LIQUID,
}

/**
 * The one container primitive. Everything else is a convenience over this.
 *
 * All three tiers now render as Material 3 [Card] with tier-driven
 * elevation + opacity. The [LIQUID] tier adds an accent tint so live
 * elements still stand out visually, but the expensive liquid-glass
 * rendering (refraction + specular + chromatic aberration) has been
 * removed.
 *
 * @param tier              semantic treatment (see [SurfaceTier])
 * @param cornerRadius      corner radius, defaults to [AstraShapes.lg]
 * @param accent            optional tint blended into the surface
 * @param contentPadding    inner padding
 * @param enablePressEffect accepted for API compatibility; no-op (liquid
 *                          press effect removed)
 */
@Composable
fun AstraSurface(
    modifier: Modifier = Modifier,
    tier: SurfaceTier = SurfaceTier.CONTENT,
    cornerRadius: Dp = AstraShapes.lg,
    accent: Color = Color.Transparent,
    contentPadding: Dp = 16.dp,
    @Suppress("UNUSED_PARAMETER") enablePressEffect: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (elevation, baseAlpha) = when (tier) {
        SurfaceTier.CONTENT -> 1.dp to 0.45f
        SurfaceTier.ELEVATED -> 3.dp to 0.7f
        SurfaceTier.LIQUID -> 4.dp to 0.75f
    }

    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = baseAlpha)
    val containerColor = if (accent != Color.Transparent) {
        baseColor.compositeOver(accent)
    } else {
        baseColor
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * Composite [accent] over this [base] color. Used by [AstraSurface] to
 * blend the accent tint into the surface background without requiring
 * the old liquid-glass rendering pipeline.
 */
private fun Color.compositeOver(accent: Color): Color {
    val alpha = accent.alpha
    if (alpha <= 0f) return this
    return Color(
        red = red * (1 - alpha) + accent.red * alpha,
        green = green * (1 - alpha) + accent.green * alpha,
        blue = blue * (1 - alpha) + accent.blue * alpha,
        alpha = 1f,
    )
}

/**
 * The everyday card. Identical to [AstraSurface]; kept as the familiar
 * name used across screens.
 */
@Composable
fun AstraCard(
    modifier: Modifier = Modifier,
    tier: SurfaceTier = SurfaceTier.CONTENT,
    cornerRadius: Dp = AstraShapes.lg,
    accent: Color = Color.Transparent,
    contentPadding: Dp = 16.dp,
    enablePressEffect: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) = AstraSurface(
    modifier = modifier,
    tier = tier,
    cornerRadius = cornerRadius,
    accent = accent,
    contentPadding = contentPadding,
    enablePressEffect = enablePressEffect,
    content = content,
)
