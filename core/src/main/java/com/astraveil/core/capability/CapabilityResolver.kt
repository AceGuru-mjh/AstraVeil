package com.astraveil.core.capability

/**
 * The v3 capability resolution entry point.
 *
 * AstraVeil does NOT detect "is there root". It detects "what can the
 * device actually do" by merging five sources:
 *
 *   device + kernel + SELinux + boot + provider
 *
 * and producing one [CapabilityMatrix]. Every subsystem then reads the
 * matrix instead of re-probing. This is the core differentiator that
 * lets AstraVeil stay backend-agnostic.
 */
interface CapabilityResolver {
    /**
     * Produce the current [CapabilityMatrix] for this device. Safe to
     * call repeatedly; callers should re-resolve after a provider
     * change or a SELinux mode flip.
     */
    suspend fun resolve(): CapabilityMatrix
}
