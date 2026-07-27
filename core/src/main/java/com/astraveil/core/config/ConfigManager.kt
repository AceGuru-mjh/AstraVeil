package com.astraveil.core.config

import android.content.Context
import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persisted AstraVeil configuration.
 *
 * Stored on disk as JSON by [ConfigManager]; mutated in memory via
 * [ConfigManager.update]. All fields are mutable `var`s so the
 * `[update](block)` pattern can apply partial changes in place.
 *
 * @property daemonEnabled    Whether the background daemon should be active.
 * @property activeProvider   Name of the currently selected root provider ("none" if none).
 * @property moduleAutoStart  Whether modules should auto-start on provider connect.
 * @property dangerousApproval Master switch enabling dangerous permission grants.
 * @property logLevel         Minimum log level name (see [com.astraveil.core.logger.LogLevel]).
 */
@Serializable
data class AstraConfig(
    var daemonEnabled: Boolean = false,
    var activeProvider: String = "none",
    var moduleAutoStart: Boolean = true,
    var dangerousApproval: Boolean = false,
    var logLevel: String = "INFO",
    var authorizedPackages: Map<String, Set<String>> = emptyMap(),
)

/**
 * Loads and persists [AstraConfig] to the application's private files directory.
 *
 * All public operations are guarded by a [Mutex] so concurrent callers from
 * multiple coroutines see a consistent view of the config. The on-disk file
 * is named `astra_config.json` and lives in [Context.getFilesDir].
 *
 * I/O is performed inline on the calling coroutine; callers that may be on
 * the main thread should wrap calls in `withContext(Dispatchers.IO)`.
 *
 * @param context Application or activity context used to locate the config file.
 */
class ConfigManager(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Coroutine-aware mutex serializing all reads and writes. */
    private val mutex = Mutex()

    /** Target config file (`<filesDir>/astra_config.json`). */
    private val configFile: File
        get() = File(context.filesDir, "astra_config.json")

    /**
     * Read the config from disk, returning a default instance if the file is
     * missing or cannot be parsed.
     */
    suspend fun load(): AstraConfig = mutex.withLock { readConfigLocked() }

    /**
     * Persist [config] to disk, replacing any previous content atomically.
     */
    suspend fun save(config: AstraConfig) = mutex.withLock { writeConfigLocked(config) }

    /**
     * Atomically mutate the config under the [Mutex]: [block] receives the
     * current config, may mutate it in place, and the result is written back
     * to disk before the lock is released.
     */
    suspend fun update(block: (AstraConfig) -> Unit) = mutex.withLock {
        val current = readConfigLocked()
        block(current)
        writeConfigLocked(current)
    }

    // ---- internal helpers (must be invoked while holding [mutex]) ----

    private fun readConfigLocked(): AstraConfig {
        val file = configFile
        if (!file.exists()) return AstraConfig()
        return try {
            json.decodeFromString(AstraConfig.serializer(), file.readText())
        } catch (t: Throwable) {
            AstraLogger.w("ConfigManager", "Failed to read config: ${t.message}; using defaults.")
            AstraConfig()
        }
    }

    private fun writeConfigLocked(config: AstraConfig) {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writeText(json.encodeToString(AstraConfig.serializer(), config))
        } catch (t: Throwable) {
            AstraLogger.e("ConfigManager", "Failed to write config", t)
        }
    }
}
