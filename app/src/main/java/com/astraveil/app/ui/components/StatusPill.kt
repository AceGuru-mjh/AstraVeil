package com.astraveil.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning

/**
 * Compact status pill / badge used across AstraUI cards.
 *
 * Renders a translucent [color]-tinted chip with an optional leading [icon]
 * and bold uppercase label text. Useful for "Online", "Offline", "Active",
 * "Detected" markers.
 *
 * @param text  the label, e.g. "Online".
 * @param color the semantic colour driving background tint + foreground.
 * @param icon  optional [ImageVector]; defaults to null.
 */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text.uppercase(),
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.6.sp
            )
        )
    }
}

/** Convenience factory values for common semantic pill colours. */
object StatusPills {
    fun success(text: String, icon: ImageVector = Icons.Filled.CheckCircle) =
        PillSpec(text, AstraSuccess, icon)
    fun warning(text: String, icon: ImageVector = Icons.Filled.Warning) =
        PillSpec(text, AstraWarning, icon)
    fun error(text: String, icon: ImageVector = Icons.Filled.Error) =
        PillSpec(text, AstraError, icon)
    fun info(text: String, icon: ImageVector = Icons.Filled.Info) =
        PillSpec(text, AstraAccent, icon)
}

data class PillSpec(val text: String, val color: Color, val icon: ImageVector?)
