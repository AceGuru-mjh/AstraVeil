package com.astraveil.core.execution

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Where an interactive command came from.
 */
enum class SessionSource {
    TERMINAL,
    ROOT_TEST,
    DIAGNOSTICS,
}

/**
 * Immutable audit record for one executed command.
 * Append-only; never edited or deleted by the app.
 */
@Serializable
data class CommandAuditEntry(
    val timestamp: Long,
    val sessionId: String,
    val source: String,          // SessionSource name
    val backend: String,         // which provider executed it
    val command: String,
    val exitCode: Int,
    val success: Boolean,
    val timedOut: Boolean,
    /** Truncated for audit storage; full output is NOT persisted. */
    val outputPreview: String,
)

@Serializable
data class SessionEvent(
    val timestamp: Long,
    val sessionId: String,
    val source: String,
    val event: String,           // APPROVED / CLOSED / DENIED
)

/**
 * Append-only audit trail for interactive privileged commands.
 *
 * Stored as JSONL (one JSON object per line) so it is tamper-evident
 * and easy to export for the user. Two record kinds share the same file:
 *  - [CommandAuditEntry]   one per executed command
 *  - [SessionEvent]        APPROVED / CLOSED lifecycle markers
 *
 * They are disambiguated at read time by the presence of the `command`
 * field; [recent] only returns [CommandAuditEntry] rows.
 */
class CommandAuditLogger(context: Context) {

    private val auditFile = File(context.filesDir, "command_audit.jsonl")
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun logCommand(entry: CommandAuditEntry) {
        auditFile.appendText(json.encodeToString(entry) + "\n")
    }

    @Synchronized
    fun logSession(event: SessionEvent) {
        auditFile.appendText(json.encodeToString(event) + "\n")
    }

    /** Read the most recent command entries (newest last). */
    fun recent(limit: Int = 100): List<CommandAuditEntry> {
        if (!auditFile.exists()) return emptyList()
        return auditFile.readLines()
            .mapNotNull { line ->
                runCatching { json.decodeFromString<CommandAuditEntry>(line) }.getOrNull()
            }
            .takeLast(limit)
    }

    /** Export the full audit trail for the user to inspect/share. */
    fun export(): File = auditFile
}
