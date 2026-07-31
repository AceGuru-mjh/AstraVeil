package com.astraveil.app.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.core.logger.AstraLogger
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/**
 * A single line rendered in the terminal.
 */
data class TerminalLine(
    val type: LineType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class LineType {
    COMMAND,
    OUTPUT,
    ERROR,
    INFO,
}

data class TerminalResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
)

/**
 * ViewModel backing the Superuser Terminal.
 *
 * Two execution modes:
 *  - ROOT:  command goes through the active RootProvider (su -c).
 *  - SHELL: command runs in a plain sh -c subprocess (no root).
 */
class TerminalViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "TerminalVM"
        private const val MAX_LINES = 2000
    }

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _useRoot = MutableStateFlow(true)
    val useRoot: StateFlow<Boolean> = _useRoot.asStateFlow()

    private val _providerName = MutableStateFlow<String?>(null)
    val providerName: StateFlow<String?> = _providerName.asStateFlow()

    private val history = mutableListOf<String>()
    private var historyCursor = -1

    init {
        addLine(TerminalLine(LineType.INFO, "AstraVeil Terminal v1.1.0"))
        addLine(TerminalLine(LineType.INFO, "Type a command and press Run. Toggle ROOT/SHELL above."))
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                runCatching {
                    ProviderRegistry.detectActive()?.displayName
                }.getOrNull()
            }
            _providerName.value = name
            addLine(TerminalLine(
                LineType.INFO,
                if (name != null) "Root backend: $name" else "No root backend detected.",
            ))
        }
    }

    fun toggleRoot() {
        _useRoot.value = !_useRoot.value
        addLine(TerminalLine(
            LineType.INFO,
            "Mode: ${if (_useRoot.value) "ROOT (su)" else "SHELL (sh)"}",
        ))
    }

    fun clear() {
        _lines.value = emptyList()
    }

    fun historyPrevious(): String? {
        if (history.isEmpty()) return null
        historyCursor = (historyCursor - 1).coerceAtLeast(0)
        return history[historyCursor]
    }

    fun historyNext(): String? {
        if (history.isEmpty()) return null
        historyCursor++
        if (historyCursor >= history.size) {
            historyCursor = history.size
            return ""
        }
        return history[historyCursor]
    }

    fun executeCommand(raw: String) {
        val command = raw.trim()
        if (command.isBlank() || _isRunning.value) return

        history.add(command)
        historyCursor = history.size
        addLine(TerminalLine(
            LineType.COMMAND,
            "${if (_useRoot.value) "#" else "$"} $command",
        ))

        when (command.lowercase()) {
            "clear" -> { clear(); return }
            "exit" -> {
                addLine(TerminalLine(LineType.INFO, "Session closed."))
                return
            }
        }

        viewModelScope.launch {
            _isRunning.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    if (_useRoot.value) runAsRoot(command) else runLocal(command)
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
            return TerminalResult(
                success = false,
                stdout = "",
                stderr = "no root backend — falling back to shell\n" +
                    "(switch to SHELL mode or install Magisk/KernelSU/APatch)",
            )
        }

        val r = runCatching { provider.execute(command) }.getOrNull()
            ?: return TerminalResult(false, "", "provider execution failed")

        return TerminalResult(r.success, r.stdout, r.stderr)
    }

    private fun runLocal(command: String): TerminalResult {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(false)
                .start()
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
            result.stdout.trimEnd().lines().forEach {
                addLine(TerminalLine(LineType.OUTPUT, it))
            }
        }
        if (result.stderr.isNotBlank()) {
            result.stderr.trimEnd().lines().forEach {
                addLine(TerminalLine(LineType.ERROR, it))
            }
        }
        if (result.stdout.isBlank() && result.stderr.isBlank()) {
            addLine(TerminalLine(
                LineType.INFO,
                if (result.success) "(no output)" else "(failed, no output)",
            ))
        }
    }

    private fun addLine(line: TerminalLine) {
        _lines.update { current ->
            (current + line).takeLast(MAX_LINES)
        }
    }
}
