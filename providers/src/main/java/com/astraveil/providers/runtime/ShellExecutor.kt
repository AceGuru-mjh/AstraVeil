package com.astraveil.providers.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [CommandExecutor] that runs a command via `Runtime.exec` in a plain
 * shell context (uid 2000 on Android). Used as the base by
 * [RootCommandExecutor] which prepends `su -c`.
 */
class ShellExecutor : CommandExecutor {

    override suspend fun execute(command: String): CommandResult =
        withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                val code = process.waitFor()
                CommandResult(
                    success = code == 0,
                    output = output,
                    error = if (error.isBlank()) null else error,
                )
            } catch (e: Exception) {
                CommandResult(
                    success = false,
                    output = "",
                    error = e.message,
                )
            }
        }
}
