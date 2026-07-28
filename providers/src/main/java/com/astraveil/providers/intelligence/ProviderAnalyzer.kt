package com.astraveil.providers.intelligence

import com.astraveil.core.device.DeviceProfile
import com.astraveil.providers.ProviderCapability
import com.astraveil.providers.ProviderReport
import com.astraveil.providers.RootProvider

/**
 * Analyzes a [RootProvider] against a [DeviceProfile] to produce a
 * detailed [ProviderReport] with per-capability confidence scores.
 *
 * The analyzer is the core of Provider Intelligence — it answers:
 * "Given this device's kernel, boot state, and SELinux mode, how
 * reliably can this provider offer each capability?"
 */
interface ProviderAnalyzer {
    suspend fun analyze(provider: RootProvider, device: DeviceProfile): ProviderReport
}

/**
 * Default implementation.
 *
 * Scoring logic:
 *  - Base score per capability = 100 if advertised, 0 if not.
 *  - Deductions based on device environment:
 *    - SELinux enforcing + vendor restrictions → -10 to -30
 *    - Bootloader locked → -20 for mount/boot capabilities
 *    - Kernel lacks OverlayFS → overlayFs score = 0
 *  - Vendor-specific adjustments (Samsung Knox, Xiaomi MIUI, etc.)
 */
class DefaultProviderAnalyzer : ProviderAnalyzer {

    override suspend fun analyze(
        provider: RootProvider,
        device: DeviceProfile,
    ): ProviderReport {
        val available = provider.available()
        val caps = if (available) provider.capabilities() else emptySet()
        val info = if (available) provider.info() else com.astraveil.providers.RootInfo.none()

        val vendor = device.manufacturer.lowercase()
        val isSamsung = vendor.contains("samsung")
        val isXiaomi = vendor.contains("xiaomi") || vendor.contains("redmi")
        val isOppo = vendor.contains("oppo") || vendor.contains("realme") || vendor.contains("oneplus")
        val isVivo = vendor.contains("vivo") || vendor.contains("iqoo")

        val strengths = mutableListOf<String>()
        val limitations = mutableListOf<String>()

        // Execute score
        val executeScore = if (ProviderCapability.ROOT_EXECUTION in caps) {
            strengths.add("Root execution")
            100
        } else 0

        // Mount score
        var mountScore = if (ProviderCapability.MOUNT_NAMESPACE in caps) {
            strengths.add("Mount operations")
            95
        } else 0
        if (isSamsung && ProviderCapability.MOUNT_NAMESPACE in caps) {
            mountScore -= 10
            limitations.add("Samsung Knox may restrict mounts")
        }

        // Namespace score
        val namespaceScore = if (ProviderCapability.MOUNT_NAMESPACE in caps) {
            if (device.kernelVersion.isNotBlank()) {
                strengths.add("Namespace isolation")
                90
            } else 80
        } else 0

        // OverlayFS score
        var overlayFsScore = if (ProviderCapability.OVERLAY_FS in caps) {
            if (device.kernelOverlayFs) {
                strengths.add("OverlayFS supported")
                85
            } else {
                limitations.add("Kernel lacks OverlayFS")
                40
            }
        } else 0
        if (isXiaomi && overlayFsScore > 0) {
            overlayFsScore -= 20
            limitations.add("MIUI may restrict overlay mounts")
        }

        // Property score
        val propertyScore = if (ProviderCapability.SYSTEM_PROPERTY in caps) {
            strengths.add("Property modification")
            80
        } else 0

        // Boot score
        var bootScore = if (ProviderCapability.BOOT_PATCH in caps) {
            if (device.bootUnlocked) {
                strengths.add("Boot integration")
                75
            } else {
                limitations.add("Bootloader locked — boot patch limited")
                30
            }
        } else 0

        // SELinux adjustment
        if (device.selinuxEnforcing) {
            if (ProviderCapability.SELINUX_CONTROL !in caps) {
                limitations.add("SELinux enforcing — no policy control")
            }
        }

        // Vendor warnings
        if (isOppo) limitations.add("ColorOS may limit background daemon")
        if (isVivo) limitations.add("OriginOS may restrict processes")

        // Overall score: weighted average
        val scores = listOf(executeScore, mountScore, namespaceScore, overlayFsScore, propertyScore, bootScore)
        val nonZero = scores.filter { it > 0 }
        val overall = if (nonZero.isEmpty()) 0 else nonZero.average().toInt()

        return ProviderReport(
            providerId = provider.id,
            displayName = provider.displayName,
            version = info.version,
            detected = available,
            executeScore = executeScore,
            mountScore = mountScore.coerceIn(0, 100),
            namespaceScore = namespaceScore,
            overlayFsScore = overlayFsScore.coerceIn(0, 100),
            propertyScore = propertyScore,
            bootScore = bootScore,
            overallScore = overall,
            strengths = strengths.distinct(),
            limitations = limitations.distinct(),
        )
    }
}
