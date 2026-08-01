package com.astraveil.app.terminal

import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

enum class StepStatus { PENDING, RUNNING, PASS, FAIL, SKIPPED }

data class SelfTestStep(
    val name: String,
    val what: String,
    val status: StepStatus = StepStatus.PENDING,
    val evidence: String = "",
    val hint: String = "",
)

/**
 * Root Chain Self-Test — runs the 7 links of the terminal chain ON DEVICE
 * and reports exactly which link breaks, with real command output as evidence.
 *
 * This is Milestone 0 made self-service: instead of "go try it and guess",
 * the app tells you precisely where the chain fails.
 */
class RootChainSelfTest(private val scope: CoroutineScope) {

    private val _steps = MutableStateFlow(initialSteps())
    val steps: StateFlow<List<SelfTestStep>> = _steps.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private fun initialSteps() = listOf(
        SelfTestStep("Root backend", "detect a root provider (Magisk/KSU/APatch)"),
        SelfTestStep("su binary", "su binary is reachable"),
        SelfTestStep("One-shot su", "su -c id executes and returns output"),
        SelfTestStep("Root granted", "output contains uid=0 (real root)"),
        SelfTestStep("su policy", "AstraVeil is authorized in the backend"),
        SelfTestStep("Persistent shell", "a long-lived su shell starts and stays alive"),
        SelfTestStep("Marker round-trip", "send a command, receive output + exit code + cwd"),
    )

    private fun update(index: Int, transform: (SelfTestStep) -> SelfTestStep) {
        _steps.value = _steps.value.toMutableList().apply {
            this[index] = transform(this[index])
        }
    }

    private fun markRunning(i: Int) = update(i) { it.copy(status = StepStatus.RUNNING) }
    private fun markPass(i: Int, evidence: String) =
        update(i) { it.copy(status = StepStatus.PASS, evidence = evidence) }
    private fun markFail(i: Int, evidence: String, hint: String) =
        update(i) { it.copy(status = StepStatus.FAIL, evidence = evidence, hint = hint) }
    private fun markSkip(i: Int, why: String) =
        update(i) { it.copy(status = StepStatus.SKIPPED, evidence = why) }

    /** Run the full chain. Each step updates the StateFlow live. */
    suspend fun run() {
        if (_running.value) return
        _running.value = true
        _steps.value = initialSteps()

        var rootOutput = ""

        // ① Root backend detection
        markRunning(0)
        val backend = withContext(Dispatchers.IO) {
            runCatching { ProviderRegistry.detectActive() }.getOrNull()
        }
        if (backend != null) {
            markPass(0, "detected: ${backend.displayName}")
        } else {
            markFail(0, "no backend detected",
                "Install Magisk / KernelSU / APatch. Without a root backend " +
                    "the terminal can only run in SHELL (app-uid) mode.")
        }

        // ② su binary reachable
        markRunning(1)
        val suPath = withContext(Dispatchers.IO) { findSu() }
        if (suPath != null) {
            markPass(1, "su found at $suPath")
        } else {
            markFail(1, "no su binary in known paths",
                "A root backend is installed but its su is not on a readable " +
                    "path. Re-install the backend or check its settings.")
        }

        // ③ One-shot su executes (may trigger the Magisk grant dialog)
        markRunning(2)
        val oneShot = withContext(Dispatchers.IO) { runRaw(listOf("su", "-c", "id")) }
        if (oneShot != null && oneShot.second.isNotBlank()) {
            rootOutput = oneShot.second
            markPass(2, rootOutput.trim().lineSequence().firstOrNull() ?: "")
        } else {
            markFail(2, oneShot?.third?.ifBlank { "no output" } ?: "su failed to start",
                "su ran but produced nothing. If a grant dialog appeared, tap " +
                    "ALLOW and re-run. (Step ④ explains why.)")
        }

        // ④ Real root: uid=0   ← THE Milestone 0 moment
        markRunning(3)
        if (rootOutput.contains("uid=0")) {
            markPass(3, rootOutput.trim())
        } else {
            markFail(3, rootOutput.ifBlank { "(empty)" }.trim(),
                "su ran but you are NOT uid=0. AstraVeil has not been granted " +
                    "root — see step ⑤.")
        }

        // ⑤ AstraVeil authorized in backend
        markRunning(4)
        if (rootOutput.contains("uid=0")) {
            markPass(4, "AstraVeil holds root (uid=0)")
        } else {
            markFail(4, "not authorized",
                "Open your root manager (Magisk/KSU/APatch) → Superuser → " +
                    "find AstraVeil → set ALLOW. If it's not listed, run any " +
                    "command in the terminal to trigger the grant dialog, then " +
                    "tap ALLOW and re-run this test.")
        }

        // Only test the persistent shell if root actually works.
        if (!rootOutput.contains("uid=0")) {
            markSkip(5, "skipped — root not granted (fix ④/⑤ first)")
            markSkip(6, "skipped — root not granted (fix ④/⑤ first)")
            _running.value = false
            return
        }

        // ⑥ Persistent su shell starts and stays alive
        markRunning(5)
        val session = ShellSession(listOf("su"), scope)
        session.start()
        delay(800)
        if (session.isAlive) {
            markPass(5, "persistent su shell alive after 800ms")
        } else {
            markFail(5, "shell exited immediately",
                "One-shot su works but a persistent su shell dies. Some su " +
                    "implementations need a TTY; this is a Phase-2 (PTY) concern. " +
                    "SHELL mode still works.")
            markSkip(6, "skipped — persistent shell unavailable")
            _running.value = false
            return
        }

        // ⑦ Marker round-trip: send a command, get output + exit + cwd
        markRunning(6)
        val gotOutput = CompletableDeferred<String>()
        val gotFinish = CompletableDeferred<Pair<Int, String>>()
        val collectorJob = scope.launch {
            session.events.collect { ev ->
                when (ev) {
                    is ShellSession.Event.Output ->
                        if (ev.line.contains("astra_selftest")) gotOutput.complete(ev.line)
                    is ShellSession.Event.CommandFinished ->
                        gotFinish.complete(ev.exitCode to ev.cwd)
                    else -> {}
                }
            }
        }
        session.sendCommand("echo astra_selftest_ok")
        val out = withTimeoutOrNull(5000) { gotOutput.await() }
        val fin = withTimeoutOrNull(5000) { gotFinish.await() }
        collectorJob.cancel()
        session.kill()

        if (out != null && fin != null) {
            markPass(6, "sent echo → got output + exit=${fin.first} cwd=${fin.second}")
        } else {
            markFail(6,
                "output=${out ?: "none"} finish=${fin?.let { "exit " + it.first } ?: "none"}",
                "The shell is alive but the marker round-trip failed. Check " +
                    "that the su shell reads piped stdin.")
        }

        _running.value = false
    }

    // ---- helpers ----

    private fun findSu(): String? {
        val paths = listOf("/system/bin/su", "/sbin/su", "/system/xbin/su",
            "/debug_ramdisk/su", "/system/bin/ksud", "/system/bin/apd")
        return paths.firstOrNull { runCatching { File(it).exists() }.getOrDefault(false) }
    }

    /** Run a raw command, return (exitCode, stdout, stderr). */
    private fun runRaw(cmd: List<String>): Triple<Int, String, String>? {
        return try {
            val p = ProcessBuilder(cmd).redirectErrorStream(false).start()
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val done = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            if (!done) {
                p.destroyForcibly()
                Triple(-1, out, "timeout")
            } else {
                Triple(p.exitValue(), out, err)
            }
        } catch (e: Exception) {
            Triple(-1, "", e.message ?: "failed")
        }
    }
}
