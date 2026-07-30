package com.astraveil.app.su

import com.astraveil.core.logger.AstraLogger
import com.astraveil.providers.RootProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and writes Magisk's real su policy database.
 *
 * Magisk stores su policies in:
 *   /data/adb/magisk.db  (SQLite)
 *   Table: policies (uid, package_name, policy, until, logging, notification)
 *   Table: logs     (from_uid, package_name, app_name, action, time)
 *
 * policy values: 0=deny, 1=prompt(ask), 2=allow
 *
 * All operations go through the active RootProvider (su -c sqlite3 ...).
 * This is REAL: changing a policy here changes what happens when
 * Termux / adb shell / any app calls `su`.
 */
class MagiskSuRepository(
    private val provider: RootProvider,
) {
    companion object {
        private const val TAG = "MagiskSuRepo"
        private const val DB = "/data/adb/magisk.db"
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
        val until: Long,        // 0 = forever, else epoch seconds
        val logging: Boolean,
        val notification: Boolean,
    )

    data class SuLogEntry(
        val fromUid: Int,
        val packageName: String,
        val appName: String,
        val action: String,     // "allow" / "deny"
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
        val result = provider.execute("sqlite3 $DB \"$sql\"")

        if (!result.success) {
            AstraLogger.w(TAG, "listPolicies failed: ${result.stderr}")
            return@withContext emptyList()
        }

        result.stdout.trim().lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val p = line.split("|")
            if (p.size < 6) return@mapNotNull null
            SuPolicyEntry(
                uid = p[0].toIntOrNull() ?: return@mapNotNull null,
                packageName = p[1],
                policy = SuPolicy.entries.find { it.value == p[2].toIntOrNull() }
                    ?: SuPolicy.ASK,
                until = p[3].toLongOrNull() ?: 0L,
                logging = p[4] == "1",
                notification = p[5] == "1",
            )
        }
    }

    /**
     * Set su policy for a specific UID/package.
     * Uses INSERT OR REPLACE so it works for both new and existing entries.
     *
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
        val result = provider.execute("sqlite3 $DB \"$sql\"")
        if (!result.success) {
            AstraLogger.e(TAG, "setPolicy failed for $packageName: ${result.stderr}")
        }
        result.success
    }

    /**
     * Delete a su policy entry. The app will get "ask" behavior
     * (Magisk default for unknown UIDs).
     */
    suspend fun deletePolicy(uid: Int): Boolean = withContext(Dispatchers.IO) {
        val result = provider.execute("sqlite3 $DB \"DELETE FROM policies WHERE uid=$uid\"")
        result.success
    }

    /**
     * List recent su request logs.
     */
    suspend fun listLogs(limit: Int = 30): List<SuLogEntry> = withContext(Dispatchers.IO) {
        val sql = "SELECT from_uid, package_name, app_name, action, time " +
            "FROM logs ORDER BY time DESC LIMIT $limit"
        val result = provider.execute("sqlite3 $DB \"$sql\"")

        if (!result.success) {
            AstraLogger.w(TAG, "listLogs failed: ${result.stderr}")
            return@withContext emptyList()
        }

        result.stdout.trim().lines().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val p = line.split("|")
            if (p.size < 5) return@mapNotNull null
            SuLogEntry(
                fromUid = p[0].toIntOrNull() ?: return@mapNotNull null,
                packageName = p[1],
                appName = p[2],
                action = p[3],
                time = p[4].toLongOrNull() ?: 0L,
            )
        }
    }

    /**
     * Check if Magisk's su database exists and is readable.
     */
    suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val result = provider.execute("test -f $DB && echo ok")
        result.success && result.stdout.trim() == "ok"
    }
}
