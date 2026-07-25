package com.astraveil.modules

/**
 * Life-cycle state of an installed [AstraModule].
 *
 * Transitions are driven by [ModuleManager]:
 *
 * ```
 * INSTALLED ──enable──▶ ENABLED ──start──▶ RUNNING
 *     ▲                    │                  │
 *     │                  disable            stop
 *     │                    ▼                  │
 *     └─────────────── DISABLED ◀────────────┘
 *                          │
 *                        (fault)
 *                          ▼
 *                        ERROR
 * ```
 *
 * `ERROR` is terminal — a module in that state must be re-installed before it
 * can be enabled again.
 */
enum class ModuleState {
    /** Package unpacked and registered, but not yet enabled. */
    INSTALLED,

    /** Enabled by the user, runtime will be loaded on next `start()`. */
    ENABLED,

    /** Native runtime loaded and entry symbol invoked. */
    RUNNING,

    /** Explicitly disabled by the user; runtime will not be loaded. */
    DISABLED,

    /** Runtime reported a fault; manual re-install required. */
    ERROR
}

/**
 * An installed Astra module.
 *
 * Created by [ModuleManager.install] and mutated in-place on subsequent
 * enable / disable / start / stop / permission-grant operations. The
 * [grantedPermissions] set is the source of truth consulted by
 * [ModuleSandbox] when building a sandbox profile.
 *
 * @property id                 Stable module id (matches [ModuleManifest.name]).
 * @property manifest           Parsed `module.json` contents.
 * @property state              Current life-cycle state.
 * @property installPath        Absolute path the .avm was unpacked into.
 * @property grantedPermissions Permissions the user has approved for this
 *                             module. Subset of [ModuleManifest.permissions].
 */
data class AstraModule(
    val id: String,
    val manifest: ModuleManifest,
    val state: ModuleState,
    val installPath: String,
    val grantedPermissions: Set<String> = emptySet()
)
