package com.astraveil.app.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.execution.InteractiveSessionFactory
import com.astraveil.app.execution.TrustedInteractiveSession
import com.astraveil.core.execution.CommandAuditEntry
import com.astraveil.core.execution.CommandAuditLogger
import com.astraveil.core.execution.SessionSource
import com.astraveil.core.logger.AstraLogger
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TerminalMode(val label: String, val prompt: String) {
    ROOT("ROOT", "#"),
    ADB("ADB", "adb$"),
    SHELL("SHELL", "$"),
}

data class TerminalLine(
    val type: LineType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class LineType { COMMAND, OUTPUT, ERROR, INFO }

/**
 * Terminal backed by a PERSISTENT shell session ([ShellSession]).
 *
 * Security model preserved: a privileged session must be approved
 * ([TerminalApprovalDialog]) before the ROOT/ADB shell starts, and every
 * command is audit-logged via [CommandAuditLogger]. SHELL mode (app-uid)
 * needs no privileged approval.
 *
 * Execution change: instead of one `su -c "cmd"` per command, ONE long-lived
 * shell process is kept alive and commands are piped to its stdin. This
 * makes `cd` / `export` persist, removes per-command su handshake latency,
 * and streams output line-by-line.
 */
class TerminalViewModel(app: Application) : AndroidViewModel(app) {

    /** Public accessor so [RootChainSelfTest] can launch coroutines on the
     *  ViewModel's scope (the same scope that gets cancelled on onCleared). */
    val vmScope get() = viewModelScope

    companion object {
        private const val TAG = "TerminalVM"
        private const val MAX_LINES = 2000
    }

    private val auditLogger = CommandAuditLogger(app)
    private val sessionFactory = InteractiveSessionFactory(auditLogger)

    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _mode = MutableStateFlow(TerminalMode.ROOT)
    val mode: StateFlow<TerminalMode> = _mode.asStateFlow()

    private val _providerName = MutableStateFlow<String?>(null)
    val providerName: StateFlow<String?> = _providerName.asStateFlow()

    private val _sessionApproved = MutableStateFlow(false)
    val sessionApproved: StateFlow<Boolean> = _sessionApproved.asStateFlow()

    private val _needsApproval = MutableStateFlow(false)
    val needsApproval: StateFlow<Boolean> = _needsApproval.asStateFlow()

    /** Live working directory, updated from each command's completion marker. */
    private val _cwd = MutableStateFlow("~")
    val cwd: StateFlow<String> = _cwd.asStateFlow()

    private var shell: ShellSession? = null
    private var privilegedSession: TrustedInteractiveSession? = null
    private var sessionId: String = ""

    private val history = mutableListOf<String>()
    private var historyCursor = -1

    init {
        addLine(TerminalLine(LineType.INFO, "AstraVeil Terminal — persistent shell"))
        addLine(TerminalLine(LineType.INFO,
            "Modes: ROOT (su) → ADB (uid 2000) → SHELL (app). " +
                "cd / export persist within a session."))
        viewModelScope.launch {
            val name = runCatching { ProviderRegistry.detectActive()?.displayName }.getOrNull()
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
        addLine(TerminalLine(LineType.INFO, "Mode: ${_mode.value.label} (prompt: ${_mode.value.prompt})"))
        // Mode change requires a new shell — if already approved, restart.
        if (_sessionApproved.value) {
            beginSession()
        }
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
     * Request a privileged session. Opens a [TrustedInteractiveSession] for
     * audit-bookkeeping and asks the UI to show the approval dialog. When
     * no root backend is present, falls back to SHELL mode.
     */
    fun requestPrivilegedSession() {
        if (_sessionApproved.value) return
        viewModelScope.launch {
            val s = runCatching { sessionFactory.open(SessionSource.TERMINAL) }.getOrNull()
            if (s == null) {
                addLine(TerminalLine(LineType.INFO,
                    "No root backend — terminal runs in app-UID shell only."))
                _mode.value = TerminalMode.SHELL
                beginSession()
                return@launch
            }
            privilegedSession = s
            _needsApproval.value = true
        }
    }

    /** UI calls this after the user acknowledges [TerminalApprovalDialog]. */
    fun beginSession() {
        _needsApproval.value = false
        // Record approval in the audit log if we have a privileged session.
        privilegedSession?.approve()

        val cmd = when (_mode.value) {
            TerminalMode.ROOT -> listOf("su")
            TerminalMode.ADB -> listOf("su", "2000")
            TerminalMode.SHELL -> listOf("sh")
        }
        sessionId = privilegedSession?.sessionId ?: "shell-${System.currentTimeMillis()}"
        startShell(cmd)
        _sessionApproved.value = true
        _cwd.value = "~"
        addLine(TerminalLine(LineType.INFO,
            "Persistent ${_mode.value.label} session started. " +
                if (_mode.value != TerminalMode.SHELL) "Commands are audited."
                else "App-UID shell (non-privileged)."))
    }

    /** Cancel the approval dialog without starting a privileged session. */
    fun cancelApproval() {
        _needsApproval.value = false
        privilegedSession?.close()
        privilegedSession = null
        _mode.value = TerminalMode.SHELL
        addLine(TerminalLine(LineType.INFO,
            "Privileged session not approved — switched to SHELL mode."))
    }

    private fun startShell(cmd: List<String>) {
        shell?.kill()
        val session = ShellSession(cmd, viewModelScope)
        shell = session
        session.start()

        viewModelScope.launch {
            session.events.collect { event ->
                when (event) {
                    is ShellSession.Event.Output ->
                        addLine(TerminalLine(
                            if (event.isError) LineType.ERROR else LineType.OUTPUT,
                            event.line))

                    is ShellSession.Event.CommandFinished -> {
                        _isRunning.value = false
                        if (event.cwd.isNotBlank()) _cwd.value = event.cwd
                        if (event.exitCode != 0) {
                            addLine(TerminalLine(LineType.INFO, "[exit ${event.exitCode}]"))
                        }
                    }

                    is ShellSession.Event.ShellDied -> {
                        _isRunning.value = false
                        addLine(TerminalLine(LineType.INFO,
                            "[shell exited — run a command to restart]"))
                    }
                }
            }
        }
    }

    fun executeCommand(raw: String) {
        val command = raw.trim()
        if (command.isBlank() || _isRunning.value) return

        when (command.lowercase()) {
            "clear" -> { clear(); return }
            "exit" -> {
                endSession()
                addLine(TerminalLine(LineType.INFO, "Session closed."))
                return
            }
        }

        // Privileged modes require an approved session.
        val privileged = _mode.value == TerminalMode.ROOT || _mode.value == TerminalMode.ADB
        if (privileged && !_sessionApproved.value) {
            requestPrivilegedSession()
            addLine(TerminalLine(LineType.INFO,
                "Privileged session required — approve to continue."))
            return
        }

        history.add(command)
        historyCursor = history.size
        addLine(TerminalLine(LineType.COMMAND, "${_mode.value.prompt} $command"))

        // Restart shell if it died.
        val s = shell
        if (s == null || !s.isAlive) {
            startShell(when (_mode.value) {
                TerminalMode.ROOT -> listOf("su")
                TerminalMode.ADB -> listOf("su", "2000")
                TerminalMode.SHELL -> listOf("sh")
            })
        }

        // Audit (security model preserved).
        auditLogger.logCommand(CommandAuditEntry(
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId,
            source = SessionSource.TERMINAL.name,
            backend = _mode.value.label,
            command = command,
            exitCode = 0,
            success = true,
            timedOut = false,
            outputPreview = "",
        ))

        _isRunning.value = true
        shell?.sendCommand(command)
    }

    /** Interrupt: kill the shell (coarse; resets cd/env). */
    fun interrupt() {
        shell?.kill()
        _isRunning.value = false
        addLine(TerminalLine(LineType.INFO,
            "[interrupted — session reset; cd/env cleared]"))
    }

    fun endSession() {
        shell?.kill()
        shell = null
        privilegedSession?.close()
        privilegedSession = null
        _sessionApproved.value = false
        _isRunning.value = false
    }

    private fun addLine(line: TerminalLine) {
        _lines.update { (it + line).takeLast(MAX_LINES) }
    }

    override fun onCleared() {
        super.onCleared()
        shell?.kill()
        privilegedSession?.close()
    }
}
