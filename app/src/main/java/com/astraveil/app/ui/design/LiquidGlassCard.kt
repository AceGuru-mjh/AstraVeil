package com.astraveil.app.ui.design

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass card — legacy container, unified into [AstraCard] (P2-17).
 *
 * Delegates to [AstraCard] with `tier = SurfaceTier.LIQUID`. New code
 * should call [AstraCard] directly with the LIQUID tier; this shim
 * exists only so existing call sites keep compiling.
 *
 * The fine-grained `enableSpecular` / `enableRefraction` /
 * `enableAberration` toggles are accepted for source compatibility but
 * no longer individually controllable — the LIQUID tier always renders
 * the full treatment, which is what "alive" means.
 *
 * @deprecated Use [AstraCard] with `tier = SurfaceTier.LIQUID`.
 */
@Deprecated(
    "Unified into AstraCard (P2-17). Use AstraCard(tier = SurfaceTier.LIQUID).",
    ReplaceWith(
        "AstraCard(modifier, tier = SurfaceTier.LIQUID, accent = accent, contentPadding = contentPadding, content = content)",
    ),
)
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = AstraShapes.lg,
    accent: Color = Color.Transparent,
    @Suppress("UNUSED_PARAMETER") enableSpecular: Boolean = true,
    enablePressEffect: Boolean = true,
    @Suppress("UNUSED_PARAMETER") enableRefraction: Boolean = true,
    @Suppress("UNUSED_PARAMETER") enableAberration: Boolean = true,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) = AstraCard(
    modifier = modifier,
    tier = SurfaceTier.LIQUID,
    cornerRadius = cornerRadius,
    accent = accent,
    contentPadding = contentPadding,
    enablePressEffect = enablePressEffect,
    content = content,
)
