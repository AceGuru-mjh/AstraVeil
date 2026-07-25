package com.astraveil.core.logger

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Severity levels supported by [AstraLogger].
 *
 * Levels are ordered: a higher ordinal represents a more severe condition.
 * [AstraLogger.setMinLevel] uses the ordinal to filter entries.
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/**
 * A single immutable log record retained in the [AstraLogger] ring buffer.
 *
 * @property timestamp Wall-clock time the entry was produced (ms since epoch).
 * @property level     Severity of the entry.
 * @property tag       Logger tag (typically the producing component name).
 * @property message   Human-readable message; may include a stack trace tail.
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

/**
 * Application-wide logger.
 *
 * Wraps [android.util.Log] while keeping a small in-memory ring buffer of the
 * most recent entries so they can be surfaced in the UI or dumped to disk for
 * diagnostics. The logger is safe to call from any thread; the ring buffer is
 * guarded by an intrinsic lock on [buffer].
 *
 * Typical usage:
 * ```
 * AstraLogger.init("AstraVeil")
 * AstraLogger.i("AstraCore", "Initialized")
 * ```
 */
object AstraLogger {

    /** Maximum number of entries retained in the in-memory ring buffer. */
    private const val BUFFER_CAPACITY = 500

    private val buffer = ArrayDeque<LogEntry>(BUFFER_CAPACITY)
    private val bufferLock = Any()

    @Volatile
    private var tagPrefix: String = "AstraVeil"

    @Volatile
    private var minLevel: LogLevel = LogLevel.DEBUG

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * Initialize the logger with a tag prefix prepended to every emitted line.
     *
     * Calling this multiple times simply replaces the prefix. Safe to call
     * before any other logger method.
     *
     * @param tagPrefix Prefix applied to all subsequent log tags.
     */
    fun init(tagPrefix: String) {
        this.tagPrefix = tagPrefix
    }

    /**
     * Configure the minimum level at which entries are recorded.
     *
     * Entries whose level has a lower ordinal than [level] are dropped before
     * they reach either logcat or the ring buffer.
     *
     * @param level New minimum level.
     */
    fun setMinLevel(level: LogLevel) {
        minLevel = level
    }

    /** Log a DEBUG message. */
    fun d(tag: String, msg: String) = log(LogLevel.DEBUG, tag, msg, null)

    /** Log an INFO message. */
    fun i(tag: String, msg: String) = log(LogLevel.INFO, tag, msg, null)

    /** Log a WARN message. */
    fun w(tag: String, msg: String) = log(LogLevel.WARN, tag, msg, null)

    /**
     * Log an ERROR message, optionally with a throwable stack trace appended
     * to the message body.
     */
    fun e(tag: String, msg: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, msg, throwable)

    /**
     * Internal dispatch routine shared by every public level method.
     */
    private fun log(level: LogLevel, tag: String, msg: String, throwable: Throwable?) {
        if (level.ordinal < minLevel.ordinal) return

        val fullTag = "$tagPrefix/$tag"
        val composed = if (throwable != null) {
            "$msg\n${Log.getStackTraceString(throwable)}"
        } else {
            msg
        }

        when (level) {
            LogLevel.DEBUG -> Log.d(fullTag, composed)
            LogLevel.INFO -> Log.i(fullTag, composed)
            LogLevel.WARN -> Log.w(fullTag, composed)
            LogLevel.ERROR -> Log.e(fullTag, composed)
        }

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = composed,
        )
        synchronized(bufferLock) {
            if (buffer.size >= BUFFER_CAPACITY) buffer.removeFirst()
            buffer.addLast(entry)
        }
    }

    /**
     * Return a defensive snapshot of the recent log entries, oldest-first.
     */
    fun recentLogs(): List<LogEntry> = synchronized(bufferLock) {
        buffer.toList()
    }

    /**
     * Write the current ring buffer contents to [path] as plain text, one
     * entry per line, overwriting any previous file. Parent directories are
     * created if missing.
     *
     * @param path Absolute destination file path.
     * @return `true` if the file was written successfully, `false` on error.
     */
    fun flushToFile(path: String): Boolean {
        val snapshot = recentLogs()
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                snapshot.forEach { entry ->
                    val ts = dateFormat.format(Date(entry.timestamp))
                    writer.write("[$ts] ${entry.level.name}/${entry.tag}: ${entry.message}\n")
                }
            }
            true
        } catch (t: Throwable) {
            Log.e("$tagPrefix/AstraLogger", "flushToFile failed", t)
            false
        }
    }
}
