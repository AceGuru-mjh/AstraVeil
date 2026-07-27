package com.astraveil.core.compatibility.rules

import com.astraveil.core.compatibility.CompatibilityRule
import com.astraveil.core.compatibility.RuleResult
import com.astraveil.core.device.DeviceProfile

class KernelRule : CompatibilityRule {
    override fun evaluate(profile: DeviceProfile): RuleResult {
        var score = 0
        val warnings = mutableListOf<String>()
        if (profile.kernelOverlayFs) score += 10
        if (profile.kernelEbpf) score += 5
        if (profile.kernelLandlock) score += 5
        if (!profile.kernelOverlayFs) warnings.add("OverlayFS not detected — module mounts may fail")
        return RuleResult(score, warnings.firstOrNull())
    }
}
