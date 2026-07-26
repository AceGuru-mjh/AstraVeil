package com.astraveil.app.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The base frosted-glass surface. A rounded, semi-transparent white
 * layer with a subtle border that simulates glass edge highlight.
 *
 * All higher-level glass components ([GlassCard]) compose on top of this.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    surfaceAlpha: Float = AstraGlass.Surface.alpha,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.White.copy(alpha = surfaceAlpha))
            .border(
                width = 1.dp,
                color = AstraGlass.Border,
                shape = RoundedCornerShape(cornerRadius.dp),
            )
    ) {
        content()
    }
}
