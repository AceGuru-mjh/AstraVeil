package com.astraveil.core.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the current [AndroidProfile] for the UI + capability resolver.
 *
 * The dashboard reads `profile` to render device info; the capability
 * resolver reads it to decide device-side capabilities. Call [refresh]
 * after a provider change or boot.
 */
class DeviceRepository(
    private val detector: DeviceDetector = DeviceDetector(),
) {
    private val _profile = MutableStateFlow(AndroidProfile.empty())
    val profile: StateFlow<AndroidProfile> = _profile.asStateFlow()

    private val _compatibility = MutableStateFlow(
        CompatibilityResult(supported = true, warnings = emptyList())
    )
    val compatibility: StateFlow<CompatibilityResult> = _compatibility.asStateFlow()

    fun refresh() {
        _profile.value = detector.detect()
        _compatibility.value = CompatibilityChecker().check(_profile.value)
    }
}
