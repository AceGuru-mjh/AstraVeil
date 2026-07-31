package com.astraveil.app.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraWarning

private val TermBg = Color(0xFF0D1117)
private val TermCommand = Color(0xFF7EE787)
private val TermOutput = Color(0xFFC9D1D9)
private val TermError = Color(0xFFFF7B72)
private val TermInfo = Color(0xFF8B949E)

private val quickCommands = listOf(
    "id", "getenforce", "uname -a", "magisk -v",
    "whoami", "df -h", "getprop ro.build.version.release",
)

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = viewModel(),
) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val useRoot by viewModel.useRoot.collectAsStateWithLifecycle()
    val providerName by viewModel.providerName.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Superuser Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = providerName?.let { "backend: $it" }
                        ?: "no root backend",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstraOnSurfaceMuted,
                )
            }

            Text(
                text = if (useRoot) "ROOT" else "SHELL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (useRoot) AstraSuccess else AstraWarning,
            )
            Spacer(Modifier.width(6.dp))
            Switch(
                checked = useRoot,
                onCheckedChange = { viewModel.toggleRoot() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = AstraSuccess,
                ),
            )

            IconButton(onClick = { viewModel.clear() }) {
                Icon(
                    Icons.Filled.DeleteSweep,
                    contentDescription = "Clear",
                    tint = AstraOnSurfaceMuted,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(TermBg, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(lines) { line ->
                    TerminalLineRow(line)
                }
                if (isRunning) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = TermInfo,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("…", color = TermInfo, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickCommands.forEach { cmd ->
                QuickChip(cmd) {
                    viewModel.executeCommand(cmd)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    viewModel.historyPrevious()?.let { input = it }
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Filled.ArrowUpward, "Previous", tint = AstraOnSurfaceMuted)
            }
            IconButton(
                onClick = {
                    viewModel.historyNext()?.let { input = it }
                },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Filled.ArrowDownward, "Next", tint = AstraOnSurfaceMuted)
            }

            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (useRoot) "run as root…" else "run in shell…",
                        fontFamily = FontFamily.Monospace,
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        viewModel.executeCommand(input)
                        input = ""
                    },
                ),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    viewModel.executeCommand(input)
                    input = ""
                },
                enabled = !isRunning && input.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp),
                    ),
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Run",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun TerminalLineRow(line: TerminalLine) {
    val color = when (line.type) {
        LineType.COMMAND -> TermCommand
        LineType.OUTPUT -> TermOutput
        LineType.ERROR -> TermError
        LineType.INFO -> TermInfo
    }
    Text(
        text = line.text.ifBlank { " " },
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun QuickChip(command: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = command,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
