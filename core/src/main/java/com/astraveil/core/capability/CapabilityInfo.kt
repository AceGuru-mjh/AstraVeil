package com.astraveil.core.capability

import kotlinx.serialization.Serializable

/**
 * SELinux operational status reported by the device kernel.
 */
@Serializable
enum class SelinuxStatus {
    /** SELinux is loaded and enforcing policy. */
    ENFORCING,

    /** SELinux is loaded but only logging denials. */
    PERMISSIVE,

    /** SELinux is not loaded on this device. */
    DISABLED,

    /** Status could not be determined. */
    UNKNOWN,
}

/**
 * Snapshot of the device's root- and filesystem-related capabilities.
 *
 * Populated by [CapabilityEngine] using non-root discovery techniques so that
 * the rest of the engine can reason about which providers might be viable on
 * the current device. Every field is serializable so the snapshot can be
 * persisted, shipped over the event bus, or surfaced to the UI.
 *
 * @property androidVersion        Human-readable Android release (e.g. "14").
 * @property apiLevel              Android framework API level (`Build.VERSION.SDK_INT`).
 * @property abi                   Primary ABI reported by the device.
 * @property abis                  Full ordered ABI list (`Build.SUPPORTED_ABIS`).
 * @property kernelVersion         Parsed kernel version token from `/proc/version`.
 * @property selinuxStatus         Aggregated SELinux status.
 * @property selinuxMode           Raw mode string read from `/sys/fs/selinux/enforce` ("1"/"0"/"").
 * @property rootAvailable         `true` if any `su` binary was detected.
 * @property rootProvider          Best-effort name of the detected provider ("none" if absent).
 * @property mountCapability       `true` if `/proc/mounts` was readable.
 * @property overlayFsCapability   `true` if overlay/overlayfs filesystem is registered.
 * @property namespaceCapability   `true` if mount namespaces are available (`/proc/self/ns/mnt`).
 * @property pidNamespace          `true` if PID namespaces are available (`/proc/self/ns/pid`).
 * @property hookCapability        `true` if a hooking backend (e.g. Riru/Zygisk-style
 *                                 or a kernel hook) is available. Phase 0 always reports `false`.
 * @property deviceModel           `Build.MODEL`.
 * @property deviceManufacturer    `Build.MANUFACTURER`.
 * @property deviceBrand           `Build.BRAND`.
 * @property fingerprint           `Build.FINGERPRINT`.
 */
@Serializable
data class CapabilityInfo(
    val androidVersion: String,
    val apiLevel: Int,
    val abi: String,
    val abis: List<String>,
    val kernelVersion: String,
    val selinuxStatus: SelinuxStatus,
    val selinuxMode: String,
    val rootAvailable: Boolean,
    val rootProvider: String,
    val mountCapability: Boolean,
    val overlayFsCapability: Boolean,
    val namespaceCapability: Boolean,
    val pidNamespace: Boolean,
    val hookCapability: Boolean = false,
    val deviceModel: String,
    val deviceManufacturer: String,
    val deviceBrand: String,
    val fingerprint: String,
) {
    companion object {
        /**
         * Return a default-occupied [CapabilityInfo] used before detection
         * runs or when the engine is mocked in tests.
         */
        fun empty(): CapabilityInfo = CapabilityInfo(
            androidVersion = "",
            apiLevel = 0,
            abi = "",
            abis = emptyList(),
            kernelVersion = "",
            selinuxStatus = SelinuxStatus.UNKNOWN,
            selinuxMode = "",
            rootAvailable = false,
            rootProvider = "none",
            mountCapability = false,
            overlayFsCapability = false,
            namespaceCapability = false,
            pidNamespace = false,
            hookCapability = false,
            deviceModel = "",
            deviceManufacturer = "",
            deviceBrand = "",
            fingerprint = "",
        )
    }

    // -- UI-friendly convenience aliases ----------------------------------
    // Read-only views so Compose screens and the SDK can use concise,
    // self-documenting property names without duplicating the underlying flags.

    /** `true` when a `su` binary (any provider) is present. Alias of [rootAvailable]. */
    val isRooted: Boolean get() = rootAvailable

    /** Whether the device exposes a usable mount master. Alias of [mountCapability]. */
    val mountSupported: Boolean get() = mountCapability

    /** Whether mount namespaces are usable. Alias of [namespaceCapability]. */
    val namespaceSupported: Boolean get() = namespaceCapability

    /** Whether OverlayFS is registered. Alias of [overlayFsCapability]. */
    val overlayFsSupported: Boolean get() = overlayFsCapability

    /** Whether a hooking backend is available. Alias of [hookCapability]. */
    val hookSupported: Boolean get() = hookCapability

    /** Human-readable SELinux mode label (e.g. "Enforcing", "Permissive"). */
    val selinuxStatusLabel: String
        get() = selinuxStatus.name.lowercase().replaceFirstChar { it.uppercase() }
}
