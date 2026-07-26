package com.astraveil.core.capability

/**
 * Low-level device probe surface used by [CapabilityResolverImpl].
 *
 * Implementations read /proc, /sys, and `Build` to answer each
 * `has*` question. Splitting the detector out of the resolver lets the
 * resolver stay pure (merge five sources → matrix) while the detector
 * owns all the messy platform reads.
 */
interface CapabilityDetector {
    /** A root backend (Magisk / KernelSU / APatch / AstraRoot) is present. */
    fun hasRoot(): Boolean

    /** Mount namespaces are available (/proc/self/ns/mnt exists). */
    fun hasNamespace(): Boolean

    /** OverlayFS is registered in /proc/filesystems. */
    fun hasOverlayFs(): Boolean

    /** SELinux is loaded and its policy can be controlled. */
    fun hasSELinux(): Boolean

    /** A boot image (boot / vendor_boot / init_boot) can be patched. */
    fun hasBootPatch(): Boolean
}
