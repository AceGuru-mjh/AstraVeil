package com.astraveil.providers.runtime

import com.astraveil.core.device.DeviceProfile
import com.astraveil.providers.ProviderRegistry
import com.astraveil.providers.RootProvider
import com.astraveil.providers.selection.ProviderRequirement
import com.astraveil.providers.selection.ProviderSelector
import com.astraveil.providers.selection.ProviderSelectionResult

/**
 * v3 intelligent provider manager that uses [ProviderSelector] +
 * [ProviderRegistry.analyzeAll] to route capability requests to the
 * best available provider.
 *
 * Replaces the old "first available provider" logic with score-based
 * selection that accounts for device-specific limitations (Knox, MIUI,
 * locked bootloader, etc.).
 */
class IntelligentProviderManager(
    private val registry: ProviderRegistry = ProviderRegistry,
    private val selector: ProviderSelector = ProviderSelector(),
) {

    /**
     * Resolve [requirement] to the best [RootProvider].
     *
     * @param device the current device profile (from DeviceInspector)
     * @return the selected provider, or null if no provider satisfies
     *         the requirement
     */
    suspend fun resolve(
        requirement: ProviderRequirement,
        device: DeviceProfile,
    ): RootProvider? {
        val result = resolveWithMetadata(requirement, device) ?: return null
        return registry.byId(result.providerId)
    }

    /**
     * Resolve [requirement] and return the full [ProviderSelectionResult]
     * including score + confidence + reason for audit logging.
     */
    suspend fun resolveWithMetadata(
        requirement: ProviderRequirement,
        device: DeviceProfile,
    ): ProviderSelectionResult? {
        val reports = registry.analyzeAll(device)
        return selector.select(requirement, reports)
    }
}
