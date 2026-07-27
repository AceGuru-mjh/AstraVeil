package com.astraveil.core.device

/**
 * Bridges [DeviceInspector] (raw probes) into the v3 capability
 * resolver pipeline. Produces a [DeviceProfile] (for the UI and
 * compatibility engine) and a [DeviceCapability] (for the resolver).
 */
class DeviceDetector(
    private val inspector: DeviceInspector = DeviceInspector(),
) {
    suspend fun detect(): DeviceProfile = inspector.inspect()

    suspend fun capabilities(): DeviceCapability {
        val profile = detect()
        return DeviceCapability(
            rootAvailable = false,
            mountNamespace = profile.kernelVersion.isNotBlank(),
            overlayFs = profile.kernelOverlayFs,
            selinuxControl = profile.selinuxEnforcing,
            bootPatch = false,
        )
    }
}
