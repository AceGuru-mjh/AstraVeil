package com.astraveil.app.adb

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

data class ConsoleLine(
    val isCommand: Boolean,
    val text: String,
    val isError: Boolean = false,
)

data class AdbConsoleResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
)

@Suppress("DEPRECATION")
class AdbConsoleViewModel(app: Application) : AndroidViewModel(app) {

    private val _lines = MutableStateFlow<List<ConsoleLine>>(emptyList())
    val lines: StateFlow<List<ConsoleLine>> = _lines.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _adbStatus = MutableStateFlow<AdbManager.AdbStatus?>(null)
    val adbStatus: StateFlow<AdbManager.AdbStatus?> = _adbStatus.asStateFlow()

    private val _hasRoot = MutableStateFlow(false)
    val hasRoot: StateFlow<Boolean> = _hasRoot.asStateFlow()

    init { refreshStatus() }

    fun refreshStatus() {
        viewModelScope.launch {
            _adbStatus.value = AdbManager.detect(getApplication())
            _hasRoot.value = withContext(Dispatchers.IO) {
                val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
                val provider = info?.let { ProviderRegistry.byId(it.providerName) }
                provider != null && runCatching { provider.available() }.getOrDefault(false)
            }
        }
    }

    fun execute(raw: String) {
        val command = raw.trim()
        if (command.isBlank() || _isRunning.value) return
        addLine(ConsoleLine(isCommand = true, text = "$ $command"))
        viewModelScope.launch {
            _isRunning.value = true
            try {
                val result = withContext(Dispatchers.IO) { runAdbCommand(command) }
                if (result.stdout.isNotBlank())
                    result.stdout.trimEnd().lines().forEach { addLine(ConsoleLine(false, it)) }
                if (result.stderr.isNotBlank())
                    result.stderr.trimEnd().lines().forEach { addLine(ConsoleLine(false, it, true)) }
                if (result.stdout.isBlank() && result.stderr.isBlank())
                    addLine(ConsoleLine(false, if (result.success) "(no output)" else "(failed)"))
            } catch (t: Throwable) {
                addLine(ConsoleLine(false, t.message ?: "error", true))
            } finally { _isRunning.value = false }
        }
    }

    fun clear() { _lines.value = emptyList() }

    private suspend fun runAdbCommand(command: String): AdbConsoleResult {
        val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
        val provider = info?.let { ProviderRegistry.byId(it.providerName) }
        if (provider != null && _hasRoot.value) {
            val adbCmd = AdbManager.buildAdbShellCommand(command, hasRoot = true)
            val r = runCatching { provider.execute(adbCmd) }.getOrNull()
                ?: return AdbConsoleResult(false, "", "provider failed")
            return AdbConsoleResult(r.success, r.stdout, r.stderr)
        }
        return try {
            val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(false).start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()
            AdbConsoleResult(exit == 0, stdout, stderr + "\n(no root — app UID, not uid 2000)")
        } catch (e: Exception) {
            AdbConsoleResult(false, "", e.message ?: "shell error")
        }
    }

    private fun addLine(line: ConsoleLine) {
        _lines.update { (it + line).takeLast(500) }
    }
}
