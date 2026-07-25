package com.astraveil.providers

/**
 * THE root abstraction layer of AstraVeil.
 *
 * AstraVeil never asks "is this Magisk or KernelSU?" at a call-site. Instead,
 * every backend (Magisk, KernelSU, APatch, and the future AstraRoot) implements
 * this single interface, and the rest of the platform talks to whichever
 * implementation is currently active via [ProviderRegistry].
 *
 * **Adding a new root backend** is a one-file change: implement [RootProvider],
 * register it in `ProviderRegistry.providers`, and every consumer of the API
 * automatically picks it up. This is the central design pillar that lets
 * AstraVeil stay backend-agnostic — and the reason it is positioned as a
 * "root abstraction platform" rather than yet another Magisk fork.
 *
 * Implementations MUST be safe to construct cheaply; expensive work (file IO,
 * `su` round-trips) belongs in the `suspend` members below so it can be
 * dispatched onto an IO coroutine.
 */
interface RootProvider {

    /** Canonical machine id, e.g. `"magisk"`, `"kernelsu"`, `"apatch"`. */
    val id: String

    /** Human-readable label shown in UI, e.g. `"Magisk"`. */
    val displayName: String

    /**
     * Fast positive check — `true` if this backend is present on the device.
     *
     * Implementations should favour filesystem probes over running a shell
     * command; this method is polled by [ProviderRegistry.detectActive] and is
     * expected to return in well under a second.
     */
    suspend fun available(): Boolean

    /**
     * Full detection — read version, module path and supported features from
     * the backend's native artifacts. Returns a [RootInfo] with
     * [RootInfo.detected] = `true` when the backend is positively identified.
     */
    suspend fun detect(): RootInfo

    /**
     * Run `command` through this backend's `su` path and return the captured
     * output. Implementations must NOT echo secrets to logs.
     */
    suspend fun execute(command: String): ProviderExecResult

    /** Return the cached / last-known [RootInfo] for this provider. */
    suspend fun info(): RootInfo

    /**
     * Mount `source` onto `target` with the given mount `options` (e.g.
     * `"rw,bind"`). Returns `true` on success. Some backends will not support
     * arbitrary mounts — in that case return `false` rather than throwing.
     */
    suspend fun mount(source: String, target: String, options: String): Boolean
}

/**
 * Result of running a shell command through a [RootProvider].
 *
 * @property exitCode  Process exit code (`0` on success).
 * @property stdout    Captured standard output, never `null`.
 * @property stderr    Captured standard error, never `null`.
 * @property success   Convenience flag — `true` iff [exitCode] == `0`.
 */
data class ProviderExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val success: Boolean
)
