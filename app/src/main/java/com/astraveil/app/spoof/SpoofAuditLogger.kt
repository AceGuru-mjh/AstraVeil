package com.astraveil.app.spoof

import android.content.Context
import com.astraveil.core.execution.CommandAuditEntry
import com.astraveil.core.execution.CommandAuditLogger

object SpoofAuditLogger {

    fun logApply(
        context: Context,
        profileName: String,
        providerName: String,
        persistent: Boolean,
    ) {
        runCatching {
            CommandAuditLogger(context).logCommand(
                CommandAuditEntry(
                    timestamp = System.currentTimeMillis(),
                    sessionId = "spoof-${System.currentTimeMillis()}",
                    source = "DEVICE_SPOOF",
                    backend = providerName,
                    command = "spoof apply \"$profileName\"" +
                        if (persistent) " (persistent)" else "",
                    exitCode = 0,
                    success = true,
                    timedOut = false,
                    outputPreview = "profile=$profileName",
                )
            )
        }
    }

    fun logReset(context: Context, providerName: String) {
        runCatching {
            CommandAuditLogger(context).logCommand(
                CommandAuditEntry(
                    timestamp = System.currentTimeMillis(),
                    sessionId = "spoof-${System.currentTimeMillis()}",
                    source = "DEVICE_SPOOF",
                    backend = providerName,
                    command = "spoof reset",
                    exitCode = 0,
                    success = true,
                    timedOut = false,
                    outputPreview = "",
                )
            )
        }
    }

    fun logPerApp(
        context: Context,
        packageName: String,
        profileName: String,
    ) {
        runCatching {
            CommandAuditLogger(context).logCommand(
                CommandAuditEntry(
                    timestamp = System.currentTimeMillis(),
                    sessionId = "spoof-${System.currentTimeMillis()}",
                    source = "DEVICE_SPOOF",
                    backend = "config",
                    command = "spoof per-app $packageName -> \"$profileName\"",
                    exitCode = 0,
                    success = true,
                    timedOut = false,
                    outputPreview = "",
                )
            )
        }
    }
}
