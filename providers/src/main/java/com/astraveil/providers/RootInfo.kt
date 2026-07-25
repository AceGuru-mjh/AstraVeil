package com.astraveil.providers

import kotlinx.serialization.Serializable

/**
 * Snapshot of the currently-active root backend on this device.
 *
 * `RootInfo` is the value type produced by every [RootProvider] when AstraVeil
 * asks "who are you and what can you do?". It is intentionally serialisable so
 * that it can be persisted to disk, shipped over the IPC bridge to the daemon,
 * or surfaced to UI code without further adaptation.
 *
 * The [providerName] string is the canonical machine identifier — one of
 * `"magisk"`, `"kernelsu"`, `"apatch"`, `"astraroot"` or `"none"` — and is the
 * single source of truth used throughout AstraVeil when deciding which code
 * paths to exercise. The [supportedFeatures] set uses well-known tokens
 * (`"mount"`, `"namespace"`, `"hook"`, `"hide"`) so that feature gating also
 * stays backend-agnostic.
 *
 * @property providerName      Canonical machine id of the backend.
 * @property displayName       Human-readable label, e.g. `"Magisk"`.
 * @property version           Version string, e.g. `"26.4"`, or `"unknown"`.
 * @property versionCode       Numeric version code if known, `0` otherwise.
 * @property suAvailable       `true` if a working `su` binary is reachable.
 * @property modulePath        Default modules directory, e.g. `/data/adb/modules`.
 * @property supportedFeatures Backend capabilities — see class KDoc for tokens.
 * @property detected          `true` once a provider has been positively seen
 *                             on the device, `false` for stubs and fallbacks.
 */
@Serializable
data class RootInfo(
    val providerName: String,
    val displayName: String,
    val version: String,
    val versionCode: Int = 0,
    val suAvailable: Boolean = false,
    val modulePath: String = "",
    val supportedFeatures: Set<String> = emptySet(),
    val detected: Boolean = false
) {
    companion object {
        /** Sentinel used when no root backend is available on the device. */
        fun none(): RootInfo = RootInfo(
            providerName = "none",
            displayName = "None",
            version = "unknown",
            versionCode = 0,
            suAvailable = false,
            modulePath = "",
            supportedFeatures = emptySet(),
            detected = false
        )
    }
}
