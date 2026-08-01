package com.astraveil.app.ui.screens.superuser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import com.astraveil.app.adb.AdbConsoleViewModel
import com.astraveil.app.adb.AdbManager
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.LiquidGlass
import com.astraveil.app.ui.design.SurfaceTier
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess

private val ConsoleBg = Color(0xFF0D1117)
private val ConsoleCmd = Color(0xFF7EE787)
private val ConsoleOut = Color(0xFFC9D1D9)
private val ConsoleErr = Color(0xFFFF7B72)

@Composable
fun AdbConsoleCard(
    viewModel: AdbConsoleViewModel = viewModel(),
) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val adbStatus by viewModel.adbStatus.collectAsStateWithLifecycle()
    val hasRoot by viewModel.hasRoot.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    AstraCard(
        tier = SurfaceTier.LIQUID,
        accent = LiquidGlass.AccentViolet,
        contentPadding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ADB Shell", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.refreshStatus() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Refresh, "Refresh", tint = AstraOnSurfaceMuted, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { viewModel.clear() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteSweep, "Clear", tint = AstraOnSurfaceMuted, modifier = Modifier.size(16.dp))
            }
        }

        val status = adbStatus
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatusDot("ADB", status?.enabled == true)
            StatusDot("adbd", status?.daemonRunning == true)
            StatusDot("root", hasRoot)
            Text(
                text = when {
                    status == null -> "detecting…"
                    status.tcpPort > 0 -> "TCP :${status.tcpPort}"
                    else -> "USB"
                },
                style = MaterialTheme.typography.labelSmall, color = AstraOnSurfaceMuted,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp)
                .background(ConsoleBg, RoundedCornerShape(10.dp)).padding(10.dp)
                .verticalScroll(scrollState),
        ) {
            if (lines.isEmpty()) {
                Text(
                    text = if (hasRoot) "Ready. Commands run as uid 2000 (adb shell)."
                           else "No root — commands run as app UID (not true adb shell).",
                    color = ConsoleOut.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace, fontSize = 11.5.sp,
                )
            } else {
                Column {
                    lines.forEach { line ->
                        Text(line.text.ifBlank { " " },
                            color = when {
                                line.isCommand -> ConsoleCmd
                                line.isError -> ConsoleErr
                                else -> ConsoleOut
                            },
                            fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, lineHeight = 16.sp)
                    }
                    if (isRunning) Text("…", color = ConsoleOut, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AdbManager.quickCommands.take(6).forEach { cmd ->
                Box(modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(7.dp))
                    .clickable { viewModel.execute(cmd) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(cmd, style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("adb shell command…", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.execute(input); input = "" }),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { viewModel.execute(input); input = "" },
                enabled = !isRunning && input.isNotBlank(),
                modifier = Modifier.size(42.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(11.dp))) {
                Icon(Icons.Filled.PlayArrow, "Execute", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun StatusDot(label: String, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp)
            .background(if (active) AstraSuccess else AstraOnSurfaceMuted.copy(alpha = 0.4f), CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (active) AstraSuccess else AstraOnSurfaceMuted,
            fontFamily = FontFamily.Monospace)
    }
}
