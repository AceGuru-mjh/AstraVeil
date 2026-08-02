package com.astraveil.app.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Unified liquid glass material. Top bar and bottom navigation share this
 * style for visual consistency across the app.
 *
 * Dark semi-transparent tint (0.55) ensures icon/text contrast;
 * 28dp background blur creates the glass effect.
 */
val AstraGlassStyle: HazeStyle
    @Composable
    get() = HazeStyle(
        tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
        blurRadius = 28.dp,
    )

/** Top bar height (excludes status bar). */
val AstraTopBarHeight: Dp = 56.dp

/** Content top inset = status bar + top bar. */
@Composable
fun rememberTopInset(): Dp {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return statusBar + AstraTopBarHeight
}

/**
 * Liquid glass top bar.
 *
 * Usage: the screen's scrollable content uses Modifier.haze(state) as the
 * blur source and fills the screen. This composable overlays the top as
 * a hazeChild. Content scrolling underneath is blurred in real-time.
 *
 * @param onBack non-null shows a back button (sub-pages); null for main pages
 */
@Composable
fun AstraGlassTopBar(
    title: String,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .hazeChild(state = hazeState, style = AstraGlassStyle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBar)
                .height(AstraTopBarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            actions()
        }

        // Glass edge refraction highlight (subtle, doesn't hurt readability)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.0f),
                        ),
                    ),
                ),
        )
    }
}
// ── Pill 导航栏专用增强样式 ──
// 比标准 AstraGlassStyle 稍高 alpha + 稍大 blur，
// 因为 pill 形状面积小，需要更强的对比度。
val AstraNavPillGlassStyle: HazeStyle
    @Composable
    get() = HazeStyle(
        tint = HazeTint(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        ),
        blurRadius = 32.dp,
    )

