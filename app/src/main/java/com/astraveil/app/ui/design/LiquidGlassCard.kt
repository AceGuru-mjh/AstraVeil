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
 * Drop-in replacement for [GlassCard] with liquid glass rendering.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    accent: Color = Color.Transparent,
    enableSpecular: Boolean = true,
    enablePressEffect: Boolean = true,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    LiquidGlassSurface(
        modifier = modifier,
        cornerRadius = cornerRadius,
        accent = accent,
        enableSpecular = enableSpecular,
        enablePressEffect = enablePressEffect,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
