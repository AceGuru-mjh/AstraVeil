package com.astraveil.app.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A liquid-glass-styled horizontal divider.
 * Gradient from transparent → white → transparent for a "light seam" effect.
 */
@Composable
fun LiquidGlassDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.12f),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color,
                        color,
                        Color.Transparent,
                    ),
                    startX = 0f,
                    endX = Float.POSITIVE_INFINITY,
                ),
            ),
    )
}
