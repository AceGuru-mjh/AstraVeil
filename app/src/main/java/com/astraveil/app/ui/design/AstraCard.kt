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
 *  [LIQUID]   ALIVE. Reserved exclusively for live state — a running
 *             module, a connected daemon, an in-progress download, a
 *             live terminal. The liquid treatment (edge refraction +
 *             specular + chromatic aberration) tells the user
 *             "something is happening here."
 *
 * Liquid is deliberately SPARING. It is the most expensive surface to
 * render and loses all meaning if applied everywhere. High signal,
 * low count: aim for ≤ 6 liquid surfaces on screen at once.
 */
enum class SurfaceTier {
    CONTENT,
    ELEVATED,
    LIQUID,
}

/**
 * The one container primitive. Everything else is a convenience over this.
 *
 * Replaces the previous four-way split (GlassCard / LiquidGlassCard /
 * GlassSurface / bare M3 Card) with a single semantic API. The caller
 * declares *what kind* of surface it wants via [tier] and the platform
 * picks the rendering:
 *
 *  - [SurfaceTier.CONTENT]  → flat M3 Card, cheap, readable
 *  - [SurfaceTier.ELEVATED] → M3 Card with higher elevation + opacity
 *  - [SurfaceTier.LIQUID]   → full [LiquidGlassSurface] (refraction +
 *    specular + chromatic aberration); the "alive" treatment
 *
 * @param tier              semantic treatment (see [SurfaceTier])
 * @param cornerRadius      corner radius, defaults to [AstraShapes.lg]
 * @param accent            optional tint blended into the surface (most
 *                          meaningful on [SurfaceTier.LIQUID])
 * @param contentPadding    inner padding
 * @param enablePressEffect liquid-only: elastic press + specular follow
 */
@Composable
fun AstraSurface(
    modifier: Modifier = Modifier,
    tier: SurfaceTier = SurfaceTier.CONTENT,
    cornerRadius: Dp = AstraShapes.lg,
    accent: Color = Color.Transparent,
    contentPadding: Dp = 16.dp,
    enablePressEffect: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (tier) {
        SurfaceTier.LIQUID -> {
            // Alive: full liquid rendering (refraction + specular + aberration)
            LiquidGlassSurface(
                modifier = modifier,
                cornerRadius = cornerRadius,
                accent = accent,
                enableSpecular = true,
                enablePressEffect = enablePressEffect,
                enableRefraction = true,
                enableAberration = true,
            ) {
                Column(Modifier.padding(contentPadding), content = content)
            }
        }

        SurfaceTier.CONTENT -> {
            // Quiet: flat, readable, cheap to render
            Card(
                modifier = modifier,
                shape = RoundedCornerShape(cornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme
                        .surfaceVariant.copy(alpha = 0.45f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(contentPadding), content = content)
            }
        }

        SurfaceTier.ELEVATED -> {
            Card(
                modifier = modifier,
                shape = RoundedCornerShape(cornerRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme
                        .surfaceVariant.copy(alpha = 0.7f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Column(Modifier.padding(contentPadding), content = content)
            }
        }
    }
}

/**
 * The everyday card. Identical to [AstraSurface]; kept as the familiar
 * name used across screens. Pass [tier] = [SurfaceTier.LIQUID] ONLY for
 * live state.
 *
 * Replaces the previous plain-M3-Card [AstraCard] overload. The old
 * `containerColor` parameter is gone — tier + accent replace it so the
 * surface treatment always carries a declared semantic. Existing call
 * sites that passed only `modifier` / `contentPadding` + a trailing
 * lambda compile unchanged.
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
