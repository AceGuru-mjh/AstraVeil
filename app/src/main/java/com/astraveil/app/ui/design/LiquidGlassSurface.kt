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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass surface renderer.
 *
 * Draws 4 optical layers. Does NOT use Modifier.blur() — that would
 * blur the glass's own content. Background blur is a window-level
 * concern handled at the Activity level on API 31+.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    accent: Color = Color.Transparent,
    enableSpecular: Boolean = true,
    enablePressEffect: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)

    var pressed by remember { mutableStateOf(false) }
    var specularNorm by remember { mutableStateOf(Offset(0.5f, 0.25f)) }

    val scale by animateFloatAsState(
        targetValue = if (pressed && enablePressEffect) LiquidGlass.PressScale else 1f,
        animationSpec = spring(
            stiffness = LiquidGlass.SpringStiffness,
            dampingRatio = LiquidGlass.SpringDamping,
        ),
        label = "lgScale",
    )

    val tintAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.13f else 0.07f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.8f),
        label = "lgTint",
    )

    Box(
        modifier = modifier
            .clip(shape)
            // Layer 0: base tint
            .background(Color.White.copy(alpha = tintAlpha), shape)
            // Layer 1: vertical gradient
            .background(
                Brush.verticalGradient(
                    listOf(LiquidGlass.GradientTop, LiquidGlass.GradientBottom),
                ),
                shape,
            )
            // Accent blend
            .then(
                if (accent != Color.Transparent) Modifier.background(accent, shape)
                else Modifier,
            )
            // Layer 2 + 3: specular + border glow
            .drawWithContent {
                drawContent()
                if (enableSpecular) {
                    drawSpecular(
                        center = Offset(
                            specularNorm.x * size.width,
                            specularNorm.y * size.height,
                        ),
                        pressed = pressed,
                    )
                }
                drawInnerGlow(cornerRadius.toPx())
            }
            // Outer border
            .border(1.dp, LiquidGlass.BorderOuter, shape)
            // Press interaction
            .then(
                if (enablePressEffect) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            pressed = true
                            specularNorm = Offset(
                                (down.position.x / size.width).coerceIn(0f, 1f),
                                (down.position.y / size.height).coerceIn(0f, 1f),
                            )
                            waitForUpOrCancellation()
                            pressed = false
                        }
                    }
                } else Modifier,
            )
            // Scale
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        content()
    }
}

private fun DrawScope.drawSpecular(center: Offset, pressed: Boolean) {
    val core = if (pressed) LiquidGlass.SpecularPressed else LiquidGlass.SpecularCore
    val radius = size.minDimension * if (pressed) 0.75f else 0.55f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(core, LiquidGlass.SpecularEdge),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawInnerGlow(cornerPx: Float) {
    val sw = 1.5.dp.toPx()
    val inset = sw / 2
    drawRoundRect(
        color = LiquidGlass.BorderGlow,
        topLeft = Offset(inset, inset),
        size = Size(size.width - sw, size.height - sw),
        cornerRadius = CornerRadius(
            (cornerPx - inset).coerceAtLeast(0f),
            (cornerPx - inset).coerceAtLeast(0f),
        ),
        style = Stroke(width = sw),
    )
}
