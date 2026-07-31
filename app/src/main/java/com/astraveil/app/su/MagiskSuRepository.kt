package com.astraveil.app.su

import com.astraveil.core.logger.AstraLogger
import com.astraveil.providers.RootProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and writes Magisk's real su policy database via `magisk --sqlite`.
 *
 * Magisk does NOT ship a standalone `sqlite3` binary. The correct
 * interface is `magisk --sqlite "SQL"`, which outputs rows in
 * `key=value | key=value | ...` format (one line per row).
 *
 * All operations go through the active RootProvider (su -c magisk --sqlite ...).
 * This is REAL: changing a policy here changes what happens when
 * Termux / adb shell / any app calls `su`.
 */
class MagiskSuRepository(
    private val provider: RootProvider,
) {
    companion object {
        private const val TAG = "MagiskSuRepo"
    }

    // ---- Data models ----

    enum class SuPolicy(val value: Int, val label: String) {
        DENY(0, "Deny"),
        ASK(1, "Ask"),
        ALLOW(2, "Allow"),
    }

    data class SuPolicyEntry(
        val uid: Int,
        val packageName: String,
        val policy: SuPolicy,
        val until: Long,
        val logging: Boolean,
        val notification: Boolean,
    )

    data class SuLogEntry(
        val fromUid: Int,
        val packageName: String,
        val appName: String,
        val action: String,
        val time: Long,
    )

    // ---- Queries ----

    /**
     * List all su policies from Magisk's database.
     * Returns empty list if no root or database not found.
     */
    suspend fun listPolicies(): List<SuPolicyEntry> = withContext(Dispatchers.IO) {
        val sql = "SELECT uid, package_name, policy, until, logging, notification " +
            "FROM policies ORDER BY uid"
        val result = provider.execute("magisk --sqlite \"$sql\"")

        if (!result.success) {
            AstraLogger.w(TAG, "listPolicies failed: ${result.stderr}")
            return@withContext emptyList()
        }

        result.stdout.trim().lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val row = parseMagiskRow(line)
            SuPolicyEntry(
                uid = row["uid"]?.toIntOrNull() ?: return@mapNotNull null,
                packageName = row["package_name"] ?: return@mapNotNull null,
                policy = SuPolicy.entries.find { it.value == row["policy"]?.toIntOrNull() }
                    ?: SuPolicy.ASK,
                until = row["until"]?.toLongOrNull() ?: 0L,
                logging = row["logging"] == "1",
                notification = row["notification"] == "1",
            )
        }
    }

    /**
     * Set su policy for a specific UID/package.
     * After this call, when the app calls `su`, Magisk will check
     * this policy and act accordingly.
     */
    suspend fun setPolicy(
        uid: Int,
        packageName: String,
        policy: SuPolicy,
        until: Long = 0,
        logging: Boolean = true,
        notification: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        val sql = "INSERT OR REPLACE INTO policies " +
            "(uid, package_name, policy, until, logging, notification) " +
            "VALUES ($uid, '$packageName', ${policy.value}, $until, " +
            "${if (logging) 1 else 0}, ${if (notification) 1 else 0})"
        val result = provider.execute("magisk --sqlite \"$sql\"")
        if (!result.success) {
            AstraLogger.e(TAG, "setPolicy failed for $packageName: ${result.stderr}")
        }
        result.success
    }

    /**
     * Delete a su policy entry. The app will get "ask" behavior.
     */
    suspend fun deletePolicy(uid: Int): Boolean = withContext(Dispatchers.IO) {
        val result = provider.execute("magisk --sqlite \"DELETE FROM policies WHERE uid=$uid\"")
        result.success
    }

    /**
     * List recent su request logs.
     */
    suspend fun listLogs(limit: Int = 30): List<SuLogEntry> = withContext(Dispatchers.IO) {
        val sql = "SELECT from_uid, package_name, app_name, action, time " +
            "FROM logs ORDER BY time DESC LIMIT $limit"
        val result = provider.execute("magisk --sqlite \"$sql\"")

        if (!result.success) {
            AstraLogger.w(TAG, "listLogs failed: ${result.stderr}")
            return@withContext emptyList()
        }

        result.stdout.trim().lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val row = parseMagiskRow(line)
            SuLogEntry(
                fromUid = row["from_uid"]?.toIntOrNull() ?: return@mapNotNull null,
                packageName = row["package_name"] ?: "",
                appName = row["app_name"] ?: "",
                action = row["action"] ?: "",
                time = row["time"]?.toLongOrNull() ?: 0L,
            )
        }
    }

    /**
     * Check if Magisk's su database is accessible via `magisk --sqlite`.
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val result = provider.execute("magisk --sqlite \"SELECT count(*) FROM policies\"")
        result.success
    }

    // ---- Internal ----

    /**
     * Parse a `magisk --sqlite` output line.
     *
     * Format: `key=value | key=value | key=value`
     * Example: `uid=10123 | package_name=com.termux | policy=2 | until=0 | logging=1 | notification=1`
     */
    private fun parseMagiskRow(line: String): Map<String, String> {
        return line.split("|").mapNotNull { part ->
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2) kv[0].trim() to kv[1].trim() else null
        }.toMap()
    }
}
