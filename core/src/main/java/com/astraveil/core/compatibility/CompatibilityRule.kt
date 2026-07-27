package com.astraveil.core.compatibility

import com.astraveil.core.device.DeviceProfile

interface CompatibilityRule {
    fun evaluate(profile: DeviceProfile): RuleResult
}

data class RuleResult(
    val score: Int,
    val warning: String? = null,
)
