package com.astraveil.app.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.adb.AdbManager
import com.astraveil.core.logger.AstraLogger
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

data class TerminalLine(
    val type: LineType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class LineType { COMMAND, OUTPUT, ERROR, INFO }

data class TerminalResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
)

class TerminalViewModel(app: Application) : AndroidViewModel(app) {

    enum class TerminalMode(val label: String, val prompt: String) {
        ROOT("ROOT", "#"),
        ADB("ADB", "adb$"),
        SHELL("SHELL", "$"),
    }

    companion object {
        private const val TAG = "TerminalVM"
        private const val MAX_LINES = 2000
    }

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _mode = MutableStateFlow(TerminalMode.ROOT)
    val mode: StateFlow<TerminalMode> = _mode.asStateFlow()

    private val _providerName = MutableStateFlow<String?>(null)
    val providerName: StateFlow<String?> = _providerName.asStateFlow()

    private val history = mutableListOf<String>()
    private var historyCursor = -1

    init {
        addLine(TerminalLine(LineType.INFO, "AstraVeil Terminal v1.2.0"))
        addLine(TerminalLine(LineType.INFO,
            "Modes: ROOT (su) → ADB (uid 2000) → SHELL (app). Tap mode to cycle."))
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                runCatching { ProviderRegistry.detectActive()?.displayName }.getOrNull()
            }
            _providerName.value = name
            addLine(TerminalLine(LineType.INFO,
                if (name != null) "Root backend: $name" else "No root backend detected."))
        }
    }

    fun cycleMode() {
        _mode.value = when (_mode.value) {
            TerminalMode.ROOT -> TerminalMode.ADB
            TerminalMode.ADB -> TerminalMode.SHELL
            TerminalMode.SHELL -> TerminalMode.ROOT
        }
        addLine(TerminalLine(LineType.INFO,
            "Mode: ${_mode.value.label} (prompt: ${_mode.value.prompt})"))
    }

    fun clear() { _lines.value = emptyList() }

    fun historyPrevious(): String? {
        if (history.isEmpty()) return null
        historyCursor = (historyCursor - 1).coerceAtLeast(0)
        return history[historyCursor]
    }

    fun historyNext(): String? {
        if (history.isEmpty()) return null
        historyCursor++
        if (historyCursor >= history.size) { historyCursor = history.size; return "" }
        return history[historyCursor]
    }

    fun executeCommand(raw: String) {
        val command = raw.trim()
        if (command.isBlank() || _isRunning.value) return

        history.add(command)
        historyCursor = history.size
        val currentMode = _mode.value
        addLine(TerminalLine(LineType.COMMAND, "${currentMode.prompt} $command"))

        when (command.lowercase()) {
            "clear" -> { clear(); return }
            "exit" -> { addLine(TerminalLine(LineType.INFO, "Session closed.")); return }
        }

        viewModelScope.launch {
            _isRunning.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    when (currentMode) {
                        TerminalMode.ROOT -> runAsRoot(command)
                        TerminalMode.ADB -> runAsAdbShell(command)
                        TerminalMode.SHELL -> runLocal(command)
                    }
                }
                emitResult(result)
            } catch (t: Throwable) {
                AstraLogger.e(TAG, "command failed", t)
                addLine(TerminalLine(LineType.ERROR, t.message ?: "execution error"))
            } finally {
                _isRunning.value = false
            }
        }
    }

    private suspend fun runAsRoot(command: String): TerminalResult {
        val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
        val provider = info?.let { ProviderRegistry.byId(it.providerName) }
        if (provider == null || !runCatching { provider.available() }.getOrDefault(false)) {
            return TerminalResult(false, "",
                "no root backend — switch to SHELL mode or install Magisk/KernelSU/APatch")
        }
        val r = runCatching { provider.execute(command) }.getOrNull()
            ?: return TerminalResult(false, "", "provider execution failed")
        return TerminalResult(r.success, r.stdout, r.stderr)
    }

    private suspend fun runAsAdbShell(command: String): TerminalResult {
        val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
        val provider = info?.let { ProviderRegistry.byId(it.providerName) }
        val hasRoot = provider != null && runCatching { provider.available() }.getOrDefault(false)

        if (hasRoot && provider != null) {
            val adbCommand = AdbManager.buildAdbShellCommand(command, hasRoot = true)
            val r = runCatching { provider.execute(adbCommand) }.getOrNull()
                ?: return TerminalResult(false, "", "provider execution failed")
            return TerminalResult(r.success, r.stdout, r.stderr)
        }

        val result = runLocal(command)
        return result.copy(
            stderr = result.stderr + "\n(note: no root — running as app UID, " +
                "not uid 2000. Not a true adb shell.)",
        )
    }

    private fun runLocal(command: String): TerminalResult {
        return try {
            val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(false).start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()
            TerminalResult(exit == 0, stdout, stderr)
        } catch (e: Exception) {
            TerminalResult(false, "", e.message ?: "shell error")
        }
    }

    private fun emitResult(result: TerminalResult) {
        if (result.stdout.isNotBlank()) {
            result.stdout.trimEnd().lines().forEach { addLine(TerminalLine(LineType.OUTPUT, it)) }
        }
        if (result.stderr.isNotBlank()) {
            result.stderr.trimEnd().lines().forEach { addLine(TerminalLine(LineType.ERROR, it)) }
        }
        if (result.stdout.isBlank() && result.stderr.isBlank()) {
            addLine(TerminalLine(LineType.INFO,
                if (result.success) "(no output)" else "(failed, no output)"))
        }
    }

    private fun addLine(line: TerminalLine) {
        _lines.update { current -> (current + line).takeLast(MAX_LINES) }
    }
}
