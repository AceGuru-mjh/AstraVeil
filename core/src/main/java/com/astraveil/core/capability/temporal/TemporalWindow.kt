package com.astraveil.core.capability.temporal

import com.astraveil.core.logger.AstraLogger
import kotlinx.serialization.Serializable
import java.util.Calendar

/**
 * A periodic temporal window during which a capability is active.
 *
 * Unlike [com.astraveil.core.capability.lease.CapabilityLease] (continuous
 * interval from grantedAt to expiresAt), TemporalWindow defines PERIODIC
 * activation windows using cron-like semantics.
 *
 * Example: a backup module needs file_write only at 2:00-4:00 AM daily:
 * ```json
 * {
 *   "temporal_windows": [
 *     { "capability": "file_write", "cron": "0 2 * * *", "duration_minutes": 120 }
 *   ]
 * }
 * ```
 *
 * The capability is ONLY active during the window. Outside the window,
 * PolicyBridge denies execution even if the module holds a valid lease.
 * This reduces the attack surface from 24/7 to the scheduled window.
 *
 * Inspired by AWS IAM Condition(DateGreaterThan) applied to OS capabilities.
 * No existing OS (seL4, Fuchsia, Linux) applies cron semantics to capability
 * management — this is a cross-domain innovation (ops scheduling → security).
 *
 * @property capability The capability this window controls.
 * @property cronMinute Minute (0-59), or -1 for "any".
 * @property cronHour Hour (0-23), or -1 for "any".
 * @property cronDayOfMonth Day of month (1-31), or -1 for "any".
 * @property cronMonth Month (1-12), or -1 for "any".
 * @property cronDayOfWeek Day of week (0=Sun..6=Sat), or -1 for "any".
 * @property durationMinutes How long the window stays open after activation.
 * @property enabled Whether this window is active.
 */
@Serializable
data class TemporalWindow(
    val capability: String,
    val cronMinute: Int = -1,
    val cronHour: Int = -1,
    val cronDayOfMonth: Int = -1,
    val cronMonth: Int = -1,
    val cronDayOfWeek: Int = -1,
    val durationMinutes: Int = 60,
    val enabled: Boolean = true,
) {
    /**
     * Check whether the capability is active at the given time.
     *
     * The window activates when ALL non-wildcard cron fields match the
     * current time, and remains active for [durationMinutes] after the
     * last matching minute.
     *
     * @param cal The time to check (defaults to now).
     * @return true if the current time falls within an active window.
     */
    fun isActive(cal: Calendar = Calendar.getInstance()): Boolean {
        if (!enabled) return false

        // Check if the current minute exactly matches the cron expression.
        if (matchesCron(cal)) return true

        // Check if we're within the duration window AFTER the last cron match.
        // Scan backwards minute-by-minute (up to durationMinutes).
        for (i in 1..durationMinutes) {
            val check = cal.clone() as Calendar
            check.add(Calendar.MINUTE, -i)
            if (matchesCron(check)) return true
        }

        return false
    }

    /**
     * Compute the next activation time from now.
     *
     * Scans forward minute-by-minute (up to 366 days) to find the next
     * cron match. Returns null if no match is found within the scan window.
     *
     * @param from Starting time (defaults to now).
     * @return The next activation time, or null.
     */
    fun nextActivation(from: Calendar = Calendar.getInstance()): Calendar? {
        val scan = from.clone() as Calendar
        scan.set(Calendar.SECOND, 0)
        scan.set(Calendar.MILLISECOND, 0)
        scan.add(Calendar.MINUTE, 1) // start from next minute

        // Scan up to 366 days * 24 hours * 60 minutes.
        val maxIterations = 366 * 24 * 60
        for (i in 0 until maxIterations) {
            if (matchesCron(scan)) return scan
            scan.add(Calendar.MINUTE, 1)
        }
        return null
    }

    /**
     * Human-readable description of the window schedule.
     */
    fun describe(): String = buildString {
        append(capability)
        append(" @ ")
        when {
            cronHour >= 0 && cronMinute >= 0 ->
                append("${cronHour.toString().padStart(2, '0')}:${cronMinute.toString().padStart(2, '0')}")
            cronHour >= 0 -> append("${cronHour.toString().padStart(2, '0')}:xx")
            cronMinute >= 0 -> append("xx:${cronMinute.toString().padStart(2, '0')}")
            else -> append("every minute")
        }
        if (cronDayOfWeek >= 0) {
            val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            append(" on ${days.getOrElse(cronDayOfWeek) { "?" }}")
        }
        if (cronDayOfMonth >= 0) append(" on day $cronDayOfMonth")
        if (cronMonth >= 0) append(" in month $cronMonth")
        append(" for ${durationMinutes}min")
    }

    private fun matchesCron(cal: Calendar): Boolean {
        val minute = cal.get(Calendar.MINUTE)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dom = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1  // Calendar.MONTH is 0-based
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1  // Calendar.SUNDAY = 1 → 0

        return matchesField(cronMinute, minute) &&
               matchesField(cronHour, hour) &&
               matchesField(cronDayOfMonth, dom) &&
               matchesField(cronMonth, month) &&
               matchesField(cronDayOfWeek, dow)
    }

    private fun matchesField(cron: Int, actual: Int): Boolean =
        cron < 0 || cron == actual

    companion object {
        private const val TAG = "TemporalWindow"

        /**
         * Parse a standard 5-field cron expression into a TemporalWindow.
         *
         * Format: "minute hour day-of-month month day-of-week"
         *
         * Examples:
         * - `"0 2 * * *"` = daily at 2:00 AM
         * - `"30 4 * * 1"` = Mondays at 4:30 AM
         * - `"0 0 1 * *"` = 1st of every month at midnight
         * - `"* * * * *"` = every minute (use with caution)
         *
         * @param capability The capability this window controls.
         * @param cron The 5-field cron expression.
         * @param durationMinutes Window duration after activation.
         */
        fun fromCron(
            capability: String,
            cron: String,
            durationMinutes: Int = 60,
        ): TemporalWindow {
            val fields = cron.trim().split("\\s+".toRegex())
            require(fields.size == 5) {
                "Cron must have exactly 5 fields (got ${fields.size}): '$cron'"
            }

            fun parseField(s: String): Int = when (s) {
                "*" -> -1
                else -> s.toIntOrNull() ?: -1
            }

            return TemporalWindow(
                capability = capability,
                cronMinute = parseField(fields[0]),
                cronHour = parseField(fields[1]),
                cronDayOfMonth = parseField(fields[2]),
                cronMonth = parseField(fields[3]),
                cronDayOfWeek = parseField(fields[4]),
                durationMinutes = durationMinutes,
            )
        }
    }
}

/**
 * Manages temporal windows for all modules.
 *
 * The TemporalWindowManager is consulted by the daemon's PolicyBridge
 * (via IPC) before allowing execution of lease-gated capabilities.
 * If a capability has a temporal window and the current time is outside
 * the window, execution is denied regardless of lease status.
 *
 * Integration point:
 * ```
 * ExecutionRouter.execute()
 *   → daemon PolicyBridge.checkWith()
 *     → LeaseTracker.hasActiveLease()     ← Innovation 1
 *     → TemporalWindowManager.isActive()  ← Innovation 9 (this)
 *     → Rust policy_check_with()
 * ```
 */
class TemporalWindowManager {

    private val windows = mutableMapOf<String, MutableList<TemporalWindow>>()

    /**
     * Register temporal windows for a module.
     *
     * @param moduleId The module these windows apply to.
     * @param moduleWindows The windows parsed from module.json.
     */
    fun register(moduleId: String, moduleWindows: List<TemporalWindow>) {
        windows[moduleId] = moduleWindows.toMutableList()
        AstraLogger.i(TAG, "Registered ${moduleWindows.size} temporal windows for '$moduleId': " +
            moduleWindows.joinToString { it.describe() })
    }

    /**
     * Unregister all windows for a module (e.g. on uninstall).
     */
    fun unregister(moduleId: String) {
        windows.remove(moduleId)
    }

    /**
     * Check whether a module's capability is within its temporal window.
     *
     * @param moduleId The module requesting execution.
     * @param capability The capability being exercised.
     * @return true if the capability is active (no window defined, or
     *         current time is within a window). If no window is defined
     *         for this capability, returns true (no restriction).
     */
    fun isActive(moduleId: String, capability: String): Boolean {
        val moduleWindows = windows[moduleId] ?: return true  // no restriction
        val capWindows = moduleWindows.filter { it.capability == capability && it.enabled }
        if (capWindows.isEmpty()) return true  // no window for this capability

        // At least one window must be active.
        return capWindows.any { it.isActive() }
    }

    /**
     * Get all registered windows (for the UI diagnostics panel).
     */
    fun allWindows(): Map<String, List<TemporalWindow>> = windows.toMap()

    /**
     * Get the next activation time for a module's capability.
     */
    fun nextActivation(moduleId: String, capability: String): java.util.Calendar? {
        val moduleWindows = windows[moduleId] ?: return null
        return moduleWindows
            .filter { it.capability == capability && it.enabled }
            .mapNotNull { it.nextActivation() }
            .minByOrNull { it.timeInMillis }
    }

    companion object {
        private const val TAG = "TemporalWindowManager"
    }
}
