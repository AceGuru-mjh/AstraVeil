package com.astraveil.app.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.OutputStream

/**
 * A persistent shell session — the core of a real terminal.
 *
 * Keeps ONE shell process alive and pipes commands into its stdin, so:
 *   - `cd` / `export` persist across commands
 *   - no per-command su handshake (fast)
 *   - output streams back line-by-line in real time
 *
 * Completion detection WITHOUT a PTY: after each command we inject
 *   echo "___ASTRA_DONE_<exit>|<cwd>___"
 * and watch for that marker. It yields the exit code AND current working
 * directory, and is stripped from displayed output.
 *
 * Honest limitation (Phase 1): full-screen interactive programs
 * (top/vi/less) and true SIGINT need a real PTY (Phase 2, native).
 * This pipe-based session covers 90% of root-management commands.
 */
class ShellSession(
    private val shellCommand: List<String>,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val MARKER_PREFIX = "___ASTRA_DONE_"
        private const val MARKER_SUFFIX = "___"
    }

    sealed class Event {
        data class Output(val line: String, val isError: Boolean) : Event()
        data class CommandFinished(val exitCode: Int, val cwd: String) : Event()
        object ShellDied : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 512)
    val events: SharedFlow<Event> = _events

    private var process: Process? = null
    private var stdin: OutputStream? = null

    @Volatile
    var isAlive: Boolean = false
        private set

    fun start() {
        if (isAlive) return
        val p = try {
            ProcessBuilder(shellCommand)
                .redirectErrorStream(false)
                .start()
        } catch (e: Exception) {
            _events.tryEmit(Event.ShellDied)
            return
        }
        process = p
        stdin = p.outputStream
        isAlive = true

        // stdout → Output events (streaming)
        scope.launch(Dispatchers.IO) {
            try {
                p.inputStream.bufferedReader().forEachLine { line ->
                    handleLine(line, isError = false)
                }
            } catch (_: Exception) { /* stream closed */ }
            onShellExit()
        }
        // stderr → Output events (error-colored)
        scope.launch(Dispatchers.IO) {
            try {
                p.errorStream.bufferedReader().forEachLine { line ->
                    handleLine(line, isError = true)
                }
            } catch (_: Exception) { /* stream closed */ }
        }
    }

    private fun handleLine(line: String, isError: Boolean) {
        // Intercept the completion marker; never display it.
        if (line.startsWith(MARKER_PREFIX) && line.endsWith(MARKER_SUFFIX)) {
            val body = line.removePrefix(MARKER_PREFIX).removeSuffix(MARKER_SUFFIX)
            val sep = body.indexOf('|')
            val code = if (sep >= 0) body.substring(0, sep).trim().toIntOrNull() ?: -1 else -1
            val cwd = if (sep >= 0) body.substring(sep + 1) else ""
            _events.tryEmit(Event.CommandFinished(code, cwd))
            return
        }
        _events.tryEmit(Event.Output(line, isError))
    }

    /** Send a command. Completion arrives as Event.CommandFinished. */
    fun sendCommand(command: String) {
        if (!isAlive) return
        // Append the marker so we learn exit code + cwd when it finishes.
        val wrapped = command + "\n" +
            "echo \"${MARKER_PREFIX}\$?|\$(pwd)${MARKER_SUFFIX}\"\n"
        runCatching {
            stdin?.apply {
                write(wrapped.toByteArray())
                flush()
            }
        }
    }

    /** Coarse interrupt: kill the shell (restart to continue). */
    fun kill() {
        isAlive = false
        runCatching { stdin?.close() }
        process?.destroy()
    }

    private fun onShellExit() {
        if (!isAlive) return
        isAlive = false
        _events.tryEmit(Event.ShellDied)
    }
}
