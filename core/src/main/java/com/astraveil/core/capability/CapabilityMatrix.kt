package com.astraveil.core.capability

import kotlinx.serialization.Serializable

/**
 * v3 capability matrix — the single output of [CapabilityResolver].
 *
 * Replaces the scattered ROOT_ACCESS / SELINUX_CONTROL / MOUNT_NAMESPACE
 * booleans that used to live ad-hoc on [CapabilityInfo]. Every v3
 * subsystem (PermissionEngine, ExecutionPipeline, AVM runtime, the UI)
 * reads capabilities from here instead of probing the device directly.
 *
 * Produced by [CapabilityResolver.resolve] from five sources:
 * device + kernel + SELinux + boot + provider.
 *
 * @property rootAccess       a root backend (any) is available
 * @property mountNamespace   mount namespaces are usable
 * @property overlayFs        OverlayFS is registered in the kernel
 * @property selinuxControl   SELinux policy can be loaded/switched
 * @property bootPatch        a boot image can be patched
 */
@Serializable
data class CapabilityMatrix(
    val rootAccess: Boolean = false,
    val mountNamespace: Boolean = false,
    val overlayFs: Boolean = false,
    val selinuxControl: Boolean = false,
    val bootPatch: Boolean = false,
) {
    companion object {
        /** Empty matrix used before resolution runs / on unrooted devices. */
        fun empty(): CapabilityMatrix = CapabilityMatrix()
    }
}
