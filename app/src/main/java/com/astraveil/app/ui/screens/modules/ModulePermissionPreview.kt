package com.astraveil.app.ui.screens.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.core.modules.model.ModulePermissionInfo

/**
 * Renders the list of permissions declared by a module manifest.
 *
 * Used inside [InstallModuleDialog] (pre-install confirmation) and the
 * module detail card. Each permission is rendered by [PermissionRow],
 * which shows:
 *  - a coloured risk dot
 *  - the capability name + human-readable reason
 *  - a risk badge ("High · 90", "Medium · 50", "Low · 20", or "Unknown")
 *
 * Patch 18.2.1: when [ModulePermissionInfo.risk] is `null` (manifest did
 * not declare a risk level), the row renders an explicit "Unknown" badge
 * with a muted dot — never a fabricated number.
 */
@Composable
fun ModulePermissionPreview(
    permissions: List<ModulePermissionInfo>,
) {
    if (permissions.isEmpty()) {
        Text(
            text = "No permissions requested.",
            style = MaterialTheme.typography.bodySmall,
            color = AstraOnSurfaceMuted,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        permissions.forEach { perm ->
            PermissionRow(perm)
        }
    }
}

@Composable
private fun PermissionRow(perm: ModulePermissionInfo) {
    val riskColor = riskColor(perm.risk)
    val riskLabel = riskLabel(perm.risk)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Risk indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = riskColor, shape = RoundedCornerShape(4.dp)),
        )

        Spacer(Modifier.width(10.dp))

        // Capability name + reason
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = perm.capability,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (perm.reason.isNotBlank()) {
                Text(
                    text = perm.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        // Risk badge — "High · 90" / "Medium · 50" / "Low · 20" / "Unknown"
        Box(
            modifier = Modifier
                .background(
                    color = riskColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = when (val r = perm.risk) {
                    null -> "Unknown"
                    else -> "$riskLabel · $r"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = riskColor,
            )
        }
    }
}

/**
 * Maps a risk score to a colour token.
 *
 * Thresholds aligned with Rust policy:
 *  - null     → Unknown  (muted gray)
 *  - 0..30    → Low      (teal)
 *  - 31..70   → Medium   (warning amber)
 *  - 71+      → High     (error red)
 */
private fun riskColor(risk: Int?): Color = when (risk) {
    null -> AstraOnSurfaceMuted
    else -> when {
        risk <= 30 -> AstraTeal
        risk <= 70 -> AstraWarning
        else -> AstraError
    }
}

private fun riskLabel(risk: Int?): String = when (risk) {
    null -> "Unknown"
    else -> when {
        risk <= 30 -> "Low"
        risk <= 70 -> "Medium"
        else -> "High"
    }
}
