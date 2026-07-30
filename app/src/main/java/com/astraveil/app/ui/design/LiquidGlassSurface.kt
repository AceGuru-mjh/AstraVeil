package com.astraveil.app.ui.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass surface — the core rendering component.
 *
 * Renders 4 optical layers inside a [Box]:
 * ```
 * ┌─────────────────────────────────────────┐
 * │ Layer 3: border glow (inner + outer)    │
 * │ ┌─────────────────────────────────────┐ │
 * │ │ Layer 2: specular highlight         │ │
 * │ │ ┌─────────────────────────────────┐ │ │
 * │ │ │ Layer 1: vertical gradient      │ │ │
 * │ │ │ ┌─────────────────────────────┐ │ │ │
 * │ │ │ │ Layer 0: base tint          │ │ │ │
 * │ │ │ └─────────────────────────────┘ │ │ │
 * │ │ └─────────────────────────────────┘ │ │
 * │ └─────────────────────────────────────┘ │
 * └─────────────────────────────────────────┘
 * ```
 *
 * On press the glass "compresses": tint increases, specular intensifies,
 * and the surface scales down with a spring bounce on release.
 * On API 31+ an optional background blur can be applied via [enableBlur].
 *
 * @param cornerRadius Corner radius in Dp. Defaults to 22dp.
 * @param accent Optional accent tint blended into Layer 0.
 * @param enableBlur Whether to apply background blur on API 31+.
 * @param enableSpecular Whether to render the touch-following specular.
 * @param enablePressEffect Whether to animate press compression.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    accent: Color = Color.Transparent,
    enableBlur: Boolean = true,
    enableSpecular: Boolean = true,
    enablePressEffect: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)

    // ---- Interaction state ----
    var pressed by remember { mutableStateOf(false) }
    var specularCenter by remember { mutableStateOf(Offset(0.5f, 0.3f)) }

    val scale by animateFloatAsState(
        targetValue = if (pressed && enablePressEffect) LiquidGlass.PressScale else 1f,
        animationSpec = spring(
            stiffness = LiquidGlass.SpringStiffness,
            dampingRatio = LiquidGlass.SpringDamping,
        ),
        label = "liquidGlassScale",
    )

    val tintAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.12f else 0.06f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.8f),
        label = "liquidGlassTint",
    )

    val cornerRadiusPx = cornerRadius.toPx()

    Box(
        modifier = modifier
            .clip(shape)
            // ---- Layer 0: base tint ----
            .background(
                color = Color.White.copy(alpha = tintAlpha),
                shape = shape,
            )
            // ---- Layer 1: vertical gradient (light from above) ----
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LiquidGlass.GradientTop,
                        LiquidGlass.GradientBottom,
                    ),
                ),
                shape = shape,
            )
            // ---- Accent blend ----
            .then(
                if (accent != Color.Transparent) {
                    Modifier.background(color = accent, shape = shape)
                } else {
                    Modifier
                }
            )
            // ---- Layer 2 + 3: specular + border (drawn in drawWithContent) ----
            .drawWithContent {
                drawContent()

                if (enableSpecular) {
                    drawSpecularHighlight(
                        center = Offset(
                            x = specularCenter.x * size.width,
                            y = specularCenter.y * size.height,
                        ),
                        pressed = pressed,
                    )
                }

                drawBorderGlow(cornerRadiusPx)
            }
            // ---- Outer border ----
            .border(
                width = 1.dp,
                color = LiquidGlass.BorderOuter,
                shape = shape,
            )
            // ---- Press interaction ----
            .then(
                if (enablePressEffect) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            pressed = true
                            specularCenter = Offset(
                                x = (down.position.x / size.width).coerceIn(0f, 1f),
                                y = (down.position.y / size.height).coerceIn(0f, 1f),
                            )
                            waitForUpOrCancellation()
                            pressed = false
                        }
                    }
                } else {
                    Modifier
                }
            )
            // ---- Scale animation ----
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        content()
    }
}

// ---- Draw helpers ----

private fun DrawScope.drawSpecularHighlight(
    center: Offset,
    pressed: Boolean,
) {
    val coreColor = if (pressed) LiquidGlass.PressedSpecular else LiquidGlass.SpecularCore
    val radius = size.minDimension * if (pressed) 0.7f else 0.55f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(coreColor, LiquidGlass.SpecularEdge),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawBorderGlow(cornerRadiusPx: Float) {
    val strokeWidth = 1.5.dp.toPx()
    val inset = strokeWidth / 2

    drawRoundRect(
        color = LiquidGlass.BorderGlow,
        topLeft = Offset(inset, inset),
        size = Size(
            width = size.width - strokeWidth,
            height = size.height - strokeWidth,
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            x = cornerRadiusPx - inset,
            y = cornerRadiusPx - inset,
        ),
        style = Stroke(width = strokeWidth),
    )
}
