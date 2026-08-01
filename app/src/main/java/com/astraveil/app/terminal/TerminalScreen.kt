package com.astraveil.app.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.astraveil.app.adb.AdbManager
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning

private val TermBg = Color(0xFF0D1117)
private val TermCommand = Color(0xFF7EE787)
private val TermOutput = Color(0xFFC9D1D9)
private val TermError = Color(0xFFFF7B72)
private val TermInfo = Color(0xFF8B949E)

private val rootQuickCommands = listOf(
    "id", "pwd", "cd /data", "getenforce", "uname -a",
    "magisk -v", "whoami", "df -h",
)

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = viewModel(),
) {
    val lines by viewModel.lines.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val providerName by viewModel.providerName.collectAsStateWithLifecycle()
    val needsApproval by viewModel.needsApproval.collectAsStateWithLifecycle()
    val sessionApproved by viewModel.sessionApproved.collectAsStateWithLifecycle()
    val cwd by viewModel.cwd.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) { listState.animateScrollToItem(lines.size - 1) }
    }

    if (needsApproval) {
        TerminalApprovalDialog(
            backendName = providerName ?: "the active root backend",
            onApproved = { viewModel.beginSession() },
            onDismiss = { viewModel.cancelApproval() },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Terminal, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Superuser Terminal",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val subtitle = buildString {
                    append(providerName?.let { "backend: $it" } ?: "no root backend")
                    if (sessionApproved) append(" · audited session")
                }
                Text(subtitle,
                    style = MaterialTheme.typography.labelSmall, color = AstraOnSurfaceMuted)
            }

            val modeColor = when (mode) {
                TerminalMode.ROOT -> AstraSuccess
                TerminalMode.ADB -> AstraWarning
                TerminalMode.SHELL -> AstraOnSurfaceMuted
            }
            Box(
                modifier = Modifier
                    .background(modeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .clickable { viewModel.cycleMode() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = modeColor,
                    fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { viewModel.clear() }) {
                Icon(Icons.Filled.DeleteSweep, "Clear", tint = AstraOnSurfaceMuted)
            }
        }

        // cwd status bar + interrupt button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TermBg.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(cwd,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AstraTeal,
                modifier = Modifier.weight(1f),
                maxLines = 1)
            if (isRunning) {
                TextButton(
                    onClick = { viewModel.interrupt() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("Interrupt", color = AstraWarning, fontSize = 11.sp)
                }
            }
        }

        // Self-test panel (Milestone 0 diagnostic)
        SelfTestPanel(viewModel = viewModel)

        // Output area
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
                .background(TermBg, RoundedCornerShape(12.dp)).padding(12.dp),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(lines) { line -> TerminalLineRow(line) }
                if (isRunning) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp),
                                color = TermInfo, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("…", color = TermInfo, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Quick commands (mode-aware)
        val quickCmds = when (mode) {
            TerminalMode.ADB -> AdbManager.quickCommands
            else -> rootQuickCommands
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickCmds.forEach { cmd -> QuickChip(cmd) { viewModel.executeCommand(cmd) } }
        }

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.historyPrevious()?.let { input = it } },
                modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowUpward, "Previous", tint = AstraOnSurfaceMuted)
            }
            IconButton(onClick = { viewModel.historyNext()?.let { input = it } },
                modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowDownward, "Next", tint = AstraOnSurfaceMuted)
            }

            TextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        when (mode) {
                            TerminalMode.ROOT -> "run as root (su)…"
                            TerminalMode.ADB -> "run as adb shell (uid 2000)…"
                            TerminalMode.SHELL -> "run in app shell…"
                        },
                        fontFamily = FontFamily.Monospace)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.executeCommand(input); input = "" }),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary),
            )

            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.executeCommand(input); input = "" },
                enabled = !isRunning && input.isNotBlank(),
                modifier = Modifier.size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
            ) {
                Icon(Icons.Filled.PlayArrow, "Run",
                    tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun SelfTestPanel(viewModel: TerminalViewModel) {
    val test = remember { RootChainSelfTest(viewModel.vmScope) }
    val steps by test.steps.collectAsStateWithLifecycle()
    val running by test.running.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide self-test" else "Root chain self-test")
            }
            Spacer(Modifier.weight(1f))
            if (expanded) {
                Button(
                    onClick = { viewModel.vmScope.launch { test.run() } },
                    enabled = !running,
                ) { Text(if (running) "Testing…" else "Run") }
            }
        }

        if (expanded) {
            AstraCard(contentPadding = 12.dp) {
                steps.forEachIndexed { i, step ->
                    SelfTestStepRow(step)
                }
            }
        }
    }
}

@Composable
private fun SelfTestStepRow(step: SelfTestStep) {
    val color = when (step.status) {
        StepStatus.PASS -> AstraSuccess
        StepStatus.FAIL -> AstraError
        StepStatus.RUNNING -> AstraTeal
        StepStatus.SKIPPED -> AstraOnSurfaceMuted
        StepStatus.PENDING -> AstraOnSurfaceMuted.copy(alpha = 0.5f)
    }
    val icon = when (step.status) {
        StepStatus.PASS -> "✓"
        StepStatus.FAIL -> "✗"
        StepStatus.RUNNING -> "…"
        StepStatus.SKIPPED -> "–"
        StepStatus.PENDING -> "·"
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, color = color, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(18.dp))
            Text(step.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (step.status == StepStatus.PENDING)
                    AstraOnSurfaceMuted else MaterialTheme.colorScheme.onSurface)
            if (step.status == StepStatus.RUNNING) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp, color = AstraTeal)
            }
        }
        if (step.evidence.isNotBlank()) {
            Text(step.evidence,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                color = color,
                maxLines = 2,
                modifier = Modifier.padding(start = 18.dp))
        }
        if (step.status == StepStatus.FAIL && step.hint.isNotBlank()) {
            Text(step.hint,
                fontSize = 10.5.sp,
                color = AstraOnSurfaceMuted,
                modifier = Modifier.padding(start = 18.dp))
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
    Text(line.text.ifBlank { " " }, color = color,
        fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth())
}

@Composable
private fun QuickChip(command: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(command, style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
