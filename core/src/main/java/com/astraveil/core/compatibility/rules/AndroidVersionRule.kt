package com.astraveil.core.compatibility.rules

import com.astraveil.core.compatibility.CompatibilityRule
import com.astraveil.core.compatibility.RuleResult
import com.astraveil.core.device.DeviceProfile

class AndroidVersionRule : CompatibilityRule {
    override fun evaluate(profile: DeviceProfile): RuleResult = when {
        profile.androidSdk >= 35 -> RuleResult(20)
        profile.androidSdk >= 29 -> RuleResult(15)
        else -> RuleResult(5, "Android version too old (SDK ${profile.androidSdk})")
    }
}
