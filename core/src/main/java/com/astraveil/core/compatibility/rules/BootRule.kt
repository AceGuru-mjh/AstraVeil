package com.astraveil.core.compatibility.rules

import com.astraveil.core.compatibility.CompatibilityRule
import com.astraveil.core.compatibility.RuleResult
import com.astraveil.core.device.DeviceProfile

class BootRule : CompatibilityRule {
    override fun evaluate(profile: DeviceProfile): RuleResult = when {
        profile.bootUnlocked -> RuleResult(15)
        else -> RuleResult(5, "Bootloader locked — root may be restricted")
    }
}
