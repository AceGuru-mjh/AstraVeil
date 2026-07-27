package com.astraveil.core.compatibility.rules

import com.astraveil.core.compatibility.CompatibilityRule
import com.astraveil.core.compatibility.RuleResult
import com.astraveil.core.device.DeviceProfile

class SelinuxRule : CompatibilityRule {
    override fun evaluate(profile: DeviceProfile): RuleResult = when {
        profile.selinuxEnforcing -> RuleResult(15)
        profile.selinuxMode == "permissive" -> RuleResult(5, "SELinux permissive — reduced security")
        else -> RuleResult(10)
    }
}
