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
import androidx.compose.ui.draw.shadow
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
import kotlin.math.max
import kotlin.math.min

/**
 * Liquid Glass surface — Android Compose port of liquid-glass-react.
 *
 * Rendering order (bottom → top):
 * ```
 *  0. shadow              lifts glass off background
 *  1. base tint            6% white
 *  2. vertical gradient    overhead light
 *  3. accent blend         optional color tint
 *  4. content              drawContent()
 *  5. edge refraction      concentric gradient strokes (simulates feDisplacementMap)
 *  6. chromatic aberration RGB-offset edge strokes
 *  7. specular highlight   radial gradient following touch
 *  8. inner border glow    1dp bright stroke
 *  9. outer border         1dp subtle stroke
 * 10. overlay border       second border layer (mix-blend simulation)
 * ```
 *
 * NO Modifier.blur() — that blurs the glass's own content into a blob.
 * Background blur is a window-level concern (API 31+ FLAG_BLUR_BEHIND).
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    accent: Color = Color.Transparent,
    enableSpecular: Boolean = true,
    enablePressEffect: Boolean = true,
    enableRefraction: Boolean = true,
    enableAberration: Boolean = true,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)

    var pressed by remember { mutableStateOf(false) }
    var specularNorm by remember { mutableStateOf(Offset(0.5f, 0.25f)) }

    // ---- Elastic scale (liquid-glass-react: elasticity + scale(0.96) on click) ----
    val scale by animateFloatAsState(
        targetValue = if (pressed && enablePressEffect) LiquidGlass.PressScale else 1f,
        animationSpec = spring(
            stiffness = LiquidGlass.SpringStiffness,
            dampingRatio = LiquidGlass.SpringDamping,
        ),
        label = "lgScale",
    )

    val tintAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.12f else 0.06f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.8f),
        label = "lgTint",
    )

    val elevation by animateFloatAsState(
        targetValue = if (pressed) LiquidGlass.ShadowPressedElevationDp
                      else LiquidGlass.ShadowElevationDp,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.75f),
        label = "lgElevation",
    )

    Box(
        modifier = modifier
            // 0. Shadow — lifts glass off background
            .shadow(
                elevation = elevation.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.15f),
            )
            .clip(shape)
            // 1. Base tint
            .background(Color.White.copy(alpha = tintAlpha), shape)
            // 2. Vertical gradient (overhead light)
            .background(
                Brush.verticalGradient(
                    listOf(LiquidGlass.GradientTop, LiquidGlass.GradientBottom),
                ),
                shape,
            )
            // 3. Accent blend
            .then(
                if (accent != Color.Transparent) Modifier.background(accent, shape)
                else Modifier,
            )
            // 4-10. Content + edge effects
            .drawWithContent {
                // 4. Content
                drawContent()

                val crPx = cornerRadius.toPx()

                // 5. Edge refraction (simulates SVG feDisplacementMap)
                if (enableRefraction) {
                    drawEdgeRefraction(crPx)
                }

                // 6. Chromatic aberration (RGB channel split at edges)
                if (enableAberration) {
                    drawChromaticAberration(crPx)
                }

                // 7. Specular highlight (follows touch)
                if (enableSpecular) {
                    drawSpecular(
                        center = Offset(
                            specularNorm.x * size.width,
                            specularNorm.y * size.height,
                        ),
                        pressed = pressed,
                    )
                }

                // 8. Inner border glow
                drawBorderStroke(
                    crPx = crPx,
                    color = LiquidGlass.BorderInner,
                    widthPx = 1.dp.toPx(),
                )

                // 10. Overlay border (second layer, simulates mix-blend-mode: overlay)
                drawBorderStroke(
                    crPx = crPx,
                    color = LiquidGlass.BorderOverlay,
                    widthPx = 2.5.dp.toPx(),
                )
            }
            // 9. Outer border
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
            // Elastic scale
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        content()
    }
}

// ================================================================
// Edge refraction — simulates SVG feDisplacementMap
//
// liquid-glass-react uses a displacement map generated from a
// rounded-rect SDF. The displacement is strongest at edges and
// zero in the center. We simulate this with concentric rounded-rect
// strokes whose alpha decays from edge → center.
// ================================================================
private fun DrawScope.drawEdgeRefraction(cornerPx: Float) {
    val steps = LiquidGlass.EdgeRefractionSteps
    val maxWidth = LiquidGlass.EdgeRefractionWidthDp * density
    val maxAlpha = LiquidGlass.EdgeRefractionAlpha

    for (i in 0 until steps) {
        // t goes from 0 (outermost edge) to 1 (innermost)
        val t = i.toFloat() / steps
        val inset = t * maxWidth
        // Alpha decays quadratically from edge to center (like SDF falloff)
        val alpha = maxAlpha * (1f - t) * (1f - t)
        if (alpha < 0.005f) continue

        val cr = max(0f, cornerPx - inset)
        drawRoundRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(inset, inset),
            size = Size(
                max(0f, size.width - 2 * inset),
                max(0f, size.height - 2 * inset),
            ),
            cornerRadius = CornerRadius(cr, cr),
            style = Stroke(width = maxWidth / steps + 0.5f),
        )
    }
}

// ================================================================
// Chromatic aberration — simulates RGB channel split
//
// liquid-glass-react offsets R/G/B channels by ±1-2px at edges
// and composites with screen blend. We draw offset colored strokes.
// ================================================================
private fun DrawScope.drawChromaticAberration(cornerPx: Float) {
    val offset = LiquidGlass.AberrationOffsetPx * density
    val sw = 1.5.dp.toPx()

    // Red channel — offset left
    drawRoundRect(
        color = LiquidGlass.AberrationRed,
        topLeft = Offset(-offset, 0f),
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
        style = Stroke(width = sw),
    )

    // Blue channel — offset right
    drawRoundRect(
        color = LiquidGlass.AberrationBlue,
        topLeft = Offset(offset, 0f),
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cornerPx, cornerPx),
        style = Stroke(width = sw),
    )
}

// ================================================================
// Specular highlight — radial gradient following touch position
// ================================================================
private fun DrawScope.drawSpecular(center: Offset, pressed: Boolean) {
    val core = if (pressed) LiquidGlass.SpecularPressed else LiquidGlass.SpecularCore
    val radius = min(size.width, size.height) * if (pressed) 0.8f else 0.6f

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

// ================================================================
// Border stroke helper
// ================================================================
private fun DrawScope.drawBorderStroke(
    crPx: Float,
    color: Color,
    widthPx: Float,
) {
    val inset = widthPx / 2
    val cr = max(0f, crPx - inset)
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(
            max(0f, size.width - widthPx),
            max(0f, size.height - widthPx),
        ),
        cornerRadius = CornerRadius(cr, cr),
        style = Stroke(width = widthPx),
    )
}
