package com.astraveil.app.ui.screens.superuser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning

@Composable
fun TerminalLauncherCard(onOpenTerminal: () -> Unit) {
    AstraCard(
        modifier = Modifier.fillMaxWidth().clickable { onOpenTerminal() },
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Terminal, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Superuser Terminal", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text("Full-screen command line with history and quick commands.",
                    style = MaterialTheme.typography.bodySmall, color = AstraOnSurfaceMuted)
            }
            Icon(Icons.Filled.OpenInNew, "Open terminal",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeBadge("ROOT", "su", AstraSuccess, Modifier.weight(1f))
            ModeBadge("ADB", "uid 2000", AstraWarning, Modifier.weight(1f))
            ModeBadge("SHELL", "app", AstraOnSurfaceMuted, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeBadge(name: String, detail: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier
        .background(color.copy(alpha = 0.10f), RoundedCornerShape(9.dp))
        .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = color)
        Text(detail, style = MaterialTheme.typography.labelSmall, color = AstraOnSurfaceMuted)
    }
}
