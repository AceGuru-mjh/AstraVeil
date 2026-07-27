package com.astraveil.core.device

import android.os.Build
import com.astraveil.core.device.boot.BootDetector
import com.astraveil.core.device.kernel.KernelDetector
import com.astraveil.core.device.selinux.SelinuxDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceInspector(
    private val kernelDetector: KernelDetector = KernelDetector(),
    private val bootDetector: BootDetector = BootDetector(),
    private val selinuxDetector: SelinuxDetector = SelinuxDetector(),
) {
    suspend fun inspect(): DeviceProfile = withContext(Dispatchers.IO) {
        val kernel = kernelDetector.detect()
        val boot = bootDetector.detect()
        val selinux = selinuxDetector.detect()

        DeviceProfile(
            manufacturer = Build.MANUFACTURER ?: "",
            brand = Build.BRAND ?: "",
            model = Build.MODEL ?: "",
            androidSdk = Build.VERSION.SDK_INT,
            androidVersion = Build.VERSION.RELEASE ?: "",
            kernelVersion = kernel.version,
            kernelOverlayFs = kernel.overlayFs,
            kernelEbpf = kernel.ebpf,
            kernelLandlock = kernel.landlock,
            bootUnlocked = boot.unlocked,
            bootVerifiedBoot = boot.verifiedBoot,
            selinuxMode = selinux.mode,
            selinuxEnforcing = selinux.enforcing,
            selinuxPolicyVersion = selinux.policyVersion,
        )
    }
}
