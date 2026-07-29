package com.astraveil.core

import android.content.Context
import com.astraveil.core.capability.CapabilityEngine
import com.astraveil.core.capability.CapabilityInfo
import com.astraveil.core.config.ConfigManager
import com.astraveil.core.event.CapabilityUpdatedEvent
import com.astraveil.core.event.EventBus
import com.astraveil.core.logger.AstraLogger
import com.astraveil.core.logger.LogLevel
import com.astraveil.core.permission.PermissionEngine
import com.astraveil.core.security.SecurityManager

/**
 * Top-level facade exposing the entire AstraVeil core engine.
 *
 * Construct one [AstraCore] per application (typically inside
 * `Application.onCreate`) and use it as the single entry point for capability
 * detection, permission brokering, configuration, logging, and security
 * primitives. The facade is intentionally a thin wiring layer — all real
 * behaviour lives in the individual engines exposed as public properties.
 *
 * Example:
 * ```
 * class AstraApp : Application() {
 *     val core = AstraCore(this)
 *     override fun onCreate() {
 *         super.onCreate()
 *         CoroutineScope(Dispatchers.Default).launch { core.initialize() }
 *     }
 * }
 * ```
 *
 * @param context Application context used for filesystem and asset access.
 */
class AstraCore(context: Context) {

    /** Shared logger instance. */
    val logger: AstraLogger = AstraLogger

    /** Persistent configuration manager. */
    val config: ConfigManager = ConfigManager(context.applicationContext)

    /** Application-wide event bus. */
    val eventBus: EventBus = EventBus

    /** Capability detection engine (root-free). */
    val capabilityEngine: CapabilityEngine = CapabilityEngine()

    /**
     * Most recently observed device capability snapshot.
     *
     * Updated automatically by [refreshCapability] and surfaced here so that
     * non-suspending callers (e.g. a Compose ViewModel) can read the current
     * value without triggering a fresh probe. Holds [CapabilityInfo.empty]
     * until the first successful scan completes.
     */
    @Volatile
    var capability: CapabilityInfo = CapabilityInfo.empty()
        private set

    /** Permission broker. */
    val permissionEngine: PermissionEngine = PermissionEngine(eventBus)

    /** Security primitives surface. */
    val security: SecurityManager = SecurityManager()

    /**
     * Initialize the engine: configure the logger, load persisted config,
     * apply the configured log level, and propagate the dangerous-approval
     * flag to the permission engine.
     *
     * Must be called once on startup before any other API is exercised.
     * Safe to call again to re-apply config (e.g. after the user changes
     * settings).
     */
    suspend fun initialize() {
        logger.init("AstraVeil")
        try {
            val cfg = config.load()
            runCatching {
                LogLevel.valueOf(cfg.logLevel.uppercase())
            }.onSuccess { level ->
                logger.setMinLevel(level)
            }.onFailure {
                logger.setMinLevel(LogLevel.INFO)
            }
            permissionEngine.setDangerousApproval(cfg.dangerousApproval)
            if (cfg.authorizedPackages.isEmpty()) {
                cfg.authorizedPackages = mapOf(
                    "com.android.shell" to setOf("su", "namespace", "mount"),
                    "com.astraveil.sample" to setOf("su", "namespace")
                )
                config.save(cfg)
            }
            permissionEngine.loadPermissions(cfg.authorizedPackages)
            logger.i("AstraCore", "Initialized; provider=${cfg.activeProvider}")
        } catch (t: Throwable) {
            logger.e("AstraCore", "initialize failed", t)
            logger.setMinLevel(LogLevel.INFO)
        }
    }

    /**
     * Update the permission status of a package/module and persist the changes.
     *
     * Uses [PermissionEngine.grant] (unconditional — caller has already
     * verified intent, e.g. the UI after prompting the user).
     */
    suspend fun updatePermission(moduleId: String, permission: String, granted: Boolean) {
        if (granted) {
            permissionEngine.grant(moduleId, permission)
        } else {
            permissionEngine.revoke(moduleId, permission)
        }
        config.update { cfg ->
            cfg.authorizedPackages = permissionEngine.dumpPermissions()
        }
    }

    /**
     * Request a permission for [moduleId] through the policy gate
     * (dangerous-permission approval check) AND persist the result.
     *
     * This is the correct entry point for module install: it respects the
     * dangerous-permission policy (unlike [updatePermission] which grants
     * unconditionally) and persists to `astra_config.json` (unlike calling
     * [PermissionEngine.request] directly, which mutates only the in-memory
     * map and loses grants on restart).
     *
     * @return `true` if the permission was granted, `false` if policy refused.
     */
    suspend fun requestAndPersistPermission(moduleId: String, permission: String): Boolean {
        val ok = permissionEngine.request(moduleId, permission)
        if (ok) {
            config.update { cfg ->
                cfg.authorizedPackages = permissionEngine.dumpPermissions()
            }
        }
        return ok
    }

    /**
     * Re-probe device capabilities and notify subscribers via
     * [CapabilityUpdatedEvent].
     *
     * @return The freshly captured [CapabilityInfo].
     */
    suspend fun refreshCapability(): CapabilityInfo {
        return try {
            val info = capabilityEngine.scan()
            capability = info
            eventBus.emit(CapabilityUpdatedEvent(info))
            info
        } catch (t: Throwable) {
            logger.e("AstraCore", "refreshCapability failed", t)
            capability
        }
    }
}
