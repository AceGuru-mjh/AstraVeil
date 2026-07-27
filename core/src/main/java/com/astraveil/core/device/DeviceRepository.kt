package com.astraveil.core.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Holds the current [DeviceProfile] for the UI + capability resolver.
 */
class DeviceRepository(
    private val detector: DeviceDetector = DeviceDetector(),
) {
    private val _profile = MutableStateFlow(DeviceProfile.empty())
    val profile: StateFlow<DeviceProfile> = _profile.asStateFlow()

    private val _compatibility = MutableStateFlow(
        com.astraveil.core.compatibility.CompatibilityResult.unknown()
    )
    val compatibility: StateFlow<com.astraveil.core.compatibility.CompatibilityResult> =
        _compatibility.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _profile.value = detector.detect()
        _compatibility.value = com.astraveil.core.compatibility.CompatibilityEngine().evaluate(_profile.value)
    }
}
