package com.astraveil.core.capability

/**
 * Default [CapabilityResolver] — delegates each capability to the
 * matching [CapabilityDetector] probe and assembles a [CapabilityMatrix].
 *
 * The existing [CapabilityEngine] remains the on-disk probe
 * implementation; this resolver is the single v3 exit point that turns
 * those probes into the matrix the rest of the platform reads.
 */
class CapabilityResolverImpl(
    private val detector: CapabilityDetector,
) : CapabilityResolver {

    override suspend fun resolve(): CapabilityMatrix {
        return CapabilityMatrix(
            rootAccess = detector.hasRoot(),
            mountNamespace = detector.hasNamespace(),
            overlayFs = detector.hasOverlayFs(),
            selinuxControl = detector.hasSELinux(),
            bootPatch = detector.hasBootPatch(),
        )
    }
}
