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
 * The base frosted-glass surface — legacy container, unified into
 * [AstraSurface] (P2-17).
 *
 * New code should use [AstraSurface] (or [AstraCard]) with an explicit
 * [SurfaceTier]. This shim keeps the original flat-glass rendering for
 * any remaining call site; it has no semantic tier and is retained
 * only for source compatibility.
 *
 * @deprecated Use [AstraSurface] with an explicit [SurfaceTier].
 */
@Deprecated(
    "Unified into AstraSurface (P2-17). Use AstraSurface(tier = SurfaceTier.CONTENT).",
    ReplaceWith("AstraSurface(modifier, tier = SurfaceTier.CONTENT, contentPadding = 0.dp) { content() }"),
)
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
