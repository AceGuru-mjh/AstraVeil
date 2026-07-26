package com.astraveil.providers

/**
 * v3 capability tokens a [RootProvider] can advertise.
 *
 * AstraVeil does not ask "is this Magisk?" — it asks "which provider
 * offers MOUNT_NAMESPACE?". Each backend declares the subset it
 * actually supports; the [ProviderRegistry] resolves a capability
 * request to the first available provider that advertises it.
 *
 * This is separate from [com.astraveil.core.capability.CapabilityMatrix]
 * (the device-side matrix) on purpose: provider capabilities describe
 * what the *backend* offers; the matrix describes what the *device*
 * can do after merging backend + kernel + SELinux + boot.
 */
enum class ProviderCapability {
    /** Can elevate to uid 0 (su). */
    ROOT_EXECUTION,

    /** Can create mount namespaces. */
    MOUNT_NAMESPACE,

    /** Can mount OverlayFS layers. */
    OVERLAY_FS,

    /** Can read/set Android system properties. */
    SYSTEM_PROPERTY,

    /** Can patch boot / vendor_boot / init_boot. */
    BOOT_PATCH,

    /** Can load / switch SELinux policy. */
    SELINUX_CONTROL,
}
