package com.astraveil.core.device

/**
 * Bridges [DeviceInspector] (raw probes) into the v3 capability
 * resolver pipeline. Produces both an [AndroidProfile] (for the UI)
 * and a [DeviceCapability] (for the resolver).
 */
class DeviceDetector(
    private val inspector: DeviceInspector = DeviceInspector(),
) {

    fun detect(): AndroidProfile = inspector.inspect()

    fun capabilities(): DeviceCapability {
        val profile = detect()
        return DeviceCapability(
            rootAvailable = false,  // provider layer fills this
            mountNamespace = profile.kernel != "unknown",
            overlayFs = false,      // kernel probe fills this
            selinuxControl = profile.selinux == "Enforcing",
            bootPatch = false,      // provider layer fills this
        )
    }
}
