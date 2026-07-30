package com.astraveil.app.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Floating pill-shaped liquid glass navigation bar.
 *
 * ```
 *  ┌─────────────────────────────────────┐
 *  │                                     │
 *  │          Screen Content             │
 *  │                                     │
 *  │   ╭───────────────────────────╮     │
 *  │   │  🏠   📦   🔍   🛡   ⚙️  │     │  ← 长条圆液态玻璃
 *  │   ╰───────────────────────────╯     │
 *  │        24dp horizontal margin       │
 *  └─────────────────────────────────────┘
 * ```
 */
data class LiquidNavItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun LiquidGlassNavigationBar(
    items: List<LiquidNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        LiquidGlassSurface(
            cornerRadius = 26.dp,
            enableSpecular = true,
            enablePressEffect = false,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    LiquidNavEntry(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onItemSelected(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.LiquidNavEntry(
    item: LiquidNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) LiquidGlass.NavIconActive else LiquidGlass.NavIconInactive,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "navIconColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "navLabelColor",
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .then(
                if (selected) Modifier.background(
                    LiquidGlass.NavSelected,
                    RoundedCornerShape(18.dp),
                ) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = labelColor,
                maxLines = 1,
            )
        }
    }
}
