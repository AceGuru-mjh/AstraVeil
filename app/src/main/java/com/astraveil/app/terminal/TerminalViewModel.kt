package com.astraveil.app.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.adb.AdbManager
import com.astraveil.app.execution.InteractiveSessionFactory
import com.astraveil.app.execution.TrustedInteractiveSession
import com.astraveil.core.execution.CommandAuditLogger
import com.astraveil.core.execution.SessionSource
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

    /**
     * `true` once the user has acknowledged the privileged-session dialog
     * (or when no root backend is present and we fall back to the local
     * app-UID shell, which needs no privileged approval).
     */
    private val _sessionApproved = MutableStateFlow(false)
    val sessionApproved: StateFlow<Boolean> = _sessionApproved.asStateFlow()

    /** `true` while a privileged session is open and the approval dialog should show. */
    private val _needsApproval = MutableStateFlow(false)
    val needsApproval: StateFlow<Boolean> = _needsApproval.asStateFlow()

    private val history = mutableListOf<String>()
    private var historyCursor = -1

    // ---- TrustedInteractiveSession plumbing (P1-12) ----
    private val auditLogger = CommandAuditLogger(app)
    private val sessionFactory = InteractiveSessionFactory(auditLogger)

    // Current privileged session; null when no root backend is present
    // or the user has not yet opened a privileged session.
    private var session: TrustedInteractiveSession? = null

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

    /**
     * Request a privileged session. Opens an unapproved
     * [TrustedInteractiveSession] against the active root backend (if any)
     * and asks the UI to show the approval dialog. When no root backend
     * is present, falls back to the local app-UID shell — no privileged
     * approval is required in that case.
     */
    fun requestPrivilegedSession() {
        if (_sessionApproved.value) return
        viewModelScope.launch {
            val s = withContext(Dispatchers.IO) {
                runCatching { sessionFactory.open(SessionSource.TERMINAL) }.getOrNull()
            }
            if (s == null) {
                addLine(TerminalLine(LineType.INFO,
                    "No root backend — terminal runs in app-UID shell only."))
                session = null
                _sessionApproved.value = true
                _needsApproval.value = false
                return@launch
            }
            session = s
            _needsApproval.value = true
        }
    }

    /** UI calls this after the user acknowledges [TerminalApprovalDialog]. */
    fun beginSession() {
        val s = session ?: run {
            _needsApproval.value = false
            _sessionApproved.value = true
            return
        }
        s.approve()
        _needsApproval.value = false
        _sessionApproved.value = true
        addLine(TerminalLine(LineType.INFO,
            "Privileged session started (backend=${s.let { "root" }}). " +
                "All commands are audited."))
    }

    /** Cancel the approval dialog without starting a privileged session. */
    fun cancelApproval() {
        _needsApproval.value = false
        session?.close()
        session = null
        // Force SHELL mode so the user is not stranded on a ROOT prompt
        // that cannot execute anything.
        _mode.value = TerminalMode.SHELL
        addLine(TerminalLine(LineType.INFO,
            "Privileged session not approved — switched to SHELL mode."))
    }

    fun endSession() {
        session?.close()
        session = null
        _sessionApproved.value = false
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
            "exit" -> {
                endSession()
                addLine(TerminalLine(LineType.INFO, "Session closed."))
                return
            }
        }

        // Privileged modes require an approved session. If the user has
        // not yet approved, surface the dialog instead of executing.
        val privileged = currentMode == TerminalMode.ROOT || currentMode == TerminalMode.ADB
        if (privileged && !_sessionApproved.value) {
            requestPrivilegedSession()
            addLine(TerminalLine(LineType.INFO,
                "Privileged session required — approve to continue."))
            return
        }

        viewModelScope.launch {
            _isRunning.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    when (currentMode) {
                        TerminalMode.ROOT -> runPrivileged(command)
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

    /**
     * ROOT mode goes through the [TrustedInteractiveSession] — this is
     * the gated, audited path. Refuses if the session is not approved.
     */
    private suspend fun runPrivileged(command: String): TerminalResult {
        val s = session
        if (s == null || !s.isApproved()) {
            return TerminalResult(false, "",
                "Privileged session not approved. Tap a ROOT command to request one.")
        }
        val r = runCatching { s.execute(command) }.getOrNull()
            ?: return TerminalResult(false, "", "provider execution failed")
        return TerminalResult(r.success, r.stdout, r.stderr)
    }

    /**
     * ADB mode wraps the user command in `su 2000 sh -c` (uid 2000 =
     * Android's adb shell uid) and runs the wrapped command through the
     * same [TrustedInteractiveSession] so it is still gated + audited.
     */
    private suspend fun runAsAdbShell(command: String): TerminalResult {
        val s = session
        if (s == null || !s.isApproved()) {
            return TerminalResult(false, "",
                "Privileged session not approved. Tap an ADB command to request one.")
        }
        val adbCommand = AdbManager.buildAdbShellCommand(command, hasRoot = true)
        val r = runCatching { s.execute(adbCommand) }.getOrNull()
            ?: return TerminalResult(false, "", "provider execution failed")
        return TerminalResult(r.success, r.stdout, r.stderr)
    }

    /**
     * SHELL mode is a plain app-UID `sh -c` — NOT privileged, so it does
     * NOT go through the [TrustedInteractiveSession] and is NOT audited
     * as a privileged command. It is the safe fallback when no root is
     * present or the user declined the privileged session.
     */
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

    override fun onCleared() {
        super.onCleared()
        session?.close()
    }
}
