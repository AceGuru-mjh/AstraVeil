package com.astraveil.providers.runtime

import com.astraveil.core.device.DeviceInspector
import com.astraveil.core.device.DeviceProfile




/**
 * Fetches real runtime data for the Dashboard.
 *
 * Replaces hardcoded `daemonOnline = true` with actual provider detection
 * and capability analysis. The daemon heartbeat will be wired when the
 * IPC layer is connected; for now, daemonOnline reflects whether the
 * provider is available (a reasonable proxy for MVP).
 */
class RuntimeRepository(
    private val deviceInspector: DeviceInspector = DeviceInspector(),
) {
    private var cachedDevice: DeviceProfile? = null

    private suspend fun device(): DeviceProfile {
        cachedDevice?.let { return it }
        val d = deviceInspector.inspect()
        cachedDevice = d
        return d
    }

    suspend fun runtimeStatus(): RuntimeStatus {
        val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
        val online = info != null && info.detected
        return RuntimeStatus(
            daemonOnline = online,
            latencyMs = if (online) 8 else 0, // TODO: real heartbeat when IPC wired
            daemonVersion = "0.1.0",
            pid = if (online) 0 else 0, // TODO: real PID from daemon
        )
    }

    suspend fun providerStatus(): ProviderStatus {
        val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
        if (info == null || !info.detected) return ProviderStatus.none()

        val dev = device()
        val reports = runCatching { ProviderRegistry.analyzeAll(dev) }.getOrNull()
        val report = reports?.firstOrNull { it.providerId == info.providerName }

        val caps = runCatching {
            ProviderRegistry.byId(info.providerName)?.capabilities()?.map { it.name } ?: emptyList()
        }.getOrNull() ?: emptyList()

        return ProviderStatus(
            name = info.displayName,
            version = info.version,
            score = report?.overallScore ?: 0,
            verified = info.detected,
            capabilities = caps,
            strengths = report?.strengths ?: emptyList(),
            limitations = report?.limitations ?: emptyList(),
        )
    }

    suspend fun capabilities(): List<CapabilityStatus> {
        val dev = device()
        val reports = runCatching { ProviderRegistry.analyzeAll(dev) }.getOrNull() ?: return emptyList()
        val info = runCatching { ProviderRegistry.detectActive() }.getOrNull()
        val activeReport = reports.firstOrNull { it.providerId == info?.providerName } ?: return emptyList()

        return listOf(
            CapabilityStatus("Execute", activeReport.executeScore > 0, activeReport.executeScore, true),
            CapabilityStatus("Mount", activeReport.mountScore > 0, activeReport.mountScore, true),
            CapabilityStatus("Namespace", activeReport.namespaceScore > 0, activeReport.namespaceScore, true),
            CapabilityStatus("OverlayFS", activeReport.overlayFsScore > 0, activeReport.overlayFsScore, true),
            CapabilityStatus("Property", activeReport.propertyScore > 0, activeReport.propertyScore, true),
            CapabilityStatus("Boot Patch", activeReport.bootScore > 0, activeReport.bootScore, true),
        )
    }
}
