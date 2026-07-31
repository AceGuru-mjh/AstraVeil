package com.astraveil.app.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astraveil.app.ui.theme.AstraWarning

/**
 * Strong acknowledgment gate shown BEFORE a privileged terminal session
 * starts. The user must check the box AND tap "I understand" — this is
 * the explicit approval that [com.astraveil.app.execution.TrustedInteractiveSession]
 * records in the audit log.
 *
 * Refusing (Cancel / back) leaves the session unapproved; [TrustedInteractiveSession.execute]
 * will throw "session not approved" if any privileged command is attempted.
 */
@Composable
fun TerminalApprovalDialog(
    backendName: String,
    onApproved: () -> Unit,
    onDismiss: () -> Unit,
) {
    var acknowledged by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Warning, null, tint = AstraWarning)
        },
        title = {
            Text("Privileged Terminal", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Commands you type here run with ROOT privilege via " +
                        "$backendName. They can modify or destroy your system.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Unlike modules (which are capability-brokered and " +
                        "sandboxed), interactive commands are NOT filtered. " +
                        "You are fully responsible for what you run.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Every command is recorded in the audit log.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = acknowledged,
                        onCheckedChange = { acknowledged = it },
                    )
                    Text(
                        "I understand and accept the risk",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onApproved,
                enabled = acknowledged,
            ) { Text("I understand") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
