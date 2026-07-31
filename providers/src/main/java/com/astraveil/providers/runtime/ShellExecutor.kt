package com.astraveil.providers.runtime

import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

object ShellExecutor {
    data class Result(val exitCode: Int, val stdout: String, val stderr: String, val timedOut: Boolean = false) {
        val success: Boolean get() = exitCode == 0 && !timedOut
    }

    private val io = Executors.newCachedThreadPool { r -> Thread(r, "astra-shell-io").apply { isDaemon = true } }

    fun run(argv: List<String>, timeoutMs: Long = 30_000): Result = try {
        val process = ProcessBuilder(argv).redirectErrorStream(false).start()
        val outF = drainAsync(process.inputStream); val errF = drainAsync(process.errorStream)
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) { process.destroyForcibly(); process.waitFor(2, TimeUnit.SECONDS); Result(-1, outF.get(), errF.get(), true) }
        else Result(process.exitValue(), outF.get(), errF.get())
    } catch (e: Exception) { Result(-1, "", e.message ?: "execution failed") }

    fun runShell(script: String, timeoutMs: Long = 30_000): Result = run(listOf("sh", "-c", script), timeoutMs)

    private fun drainAsync(stream: InputStream): Future<String> = io.submit<String> { stream.bufferedReader().use { it.readText() } }
}
