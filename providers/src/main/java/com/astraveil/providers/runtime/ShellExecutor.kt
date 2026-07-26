package com.astraveil.providers.runtime

import com.astraveil.providers.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [CommandExecutor] that runs a command via `Runtime.exec` in a plain
 * shell context (uid 2000 on Android). Used as the base by
 * [RootCommandExecutor] which prepends `su -c`.
 */
class ShellExecutor : CommandExecutor {

    override suspend fun execute(command: String): ExecutionResult =
        withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                val code = process.waitFor()
                ExecutionResult(
                    success = code == 0,
                    output = output,
                    error = if (error.isBlank()) null else error,
                )
            } catch (e: Exception) {
                ExecutionResult(
                    success = false,
                    output = "",
                    error = e.message,
                )
            }
        }
}
