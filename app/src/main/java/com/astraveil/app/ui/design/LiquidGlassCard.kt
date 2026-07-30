package com.astraveil.app.ui.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass card — drop-in replacement for [GlassCard] with
 * multi-layer optical rendering.
 *
 * Usage:
 * ```
 * LiquidGlassCard {
 *     Text("Hello from liquid glass")
 * }
 *
 * // With accent:
 * LiquidGlassCard(accent = LiquidGlass.AccentViolet) {
 *     Text("Primary action card")
 * }
 * ```
 *
 * @param cornerRadius Corner radius in Dp. Default 22dp (matches GlassCard).
 * @param accent Optional accent tint (e.g. [LiquidGlass.AccentViolet]).
 * @param enableBlur Background blur on API 31+. Default true.
 * @param enableSpecular Touch-following specular highlight. Default true.
 * @param enablePressEffect Press compression + spring bounce. Default true.
 * @param contentPadding Inner padding. Default 18dp (matches GlassCard).
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    accent: Color = Color.Transparent,
    enableBlur: Boolean = true,
    enableSpecular: Boolean = true,
    enablePressEffect: Boolean = true,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    LiquidGlassSurface(
        modifier = modifier,
        cornerRadius = cornerRadius,
        accent = accent,
        enableBlur = enableBlur,
        enableSpecular = enableSpecular,
        enablePressEffect = enablePressEffect,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
