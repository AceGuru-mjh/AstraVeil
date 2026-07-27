package com.astraveil.core.compatibility

import com.astraveil.core.compatibility.rules.AndroidVersionRule
import com.astraveil.core.compatibility.rules.BootRule
import com.astraveil.core.compatibility.rules.KernelRule
import com.astraveil.core.compatibility.rules.SelinuxRule
import com.astraveil.core.compatibility.vendors.OppoAdapter
import com.astraveil.core.compatibility.vendors.PixelAdapter
import com.astraveil.core.compatibility.vendors.SamsungAdapter
import com.astraveil.core.compatibility.vendors.VendorAdapter
import com.astraveil.core.compatibility.vendors.VivoAdapter
import com.astraveil.core.compatibility.vendors.XiaomiAdapter
import com.astraveil.core.device.DeviceProfile

class CompatibilityEngine(
    private val rules: List<CompatibilityRule> = listOf(
        AndroidVersionRule(),
        KernelRule(),
        SelinuxRule(),
        BootRule(),
    ),
    private val adapters: List<VendorAdapter> = listOf(
        PixelAdapter(),
        SamsungAdapter(),
        XiaomiAdapter(),
        OppoAdapter(),
        VivoAdapter(),
    ),
) {
    fun evaluate(profile: DeviceProfile): CompatibilityResult {
        var score = 0
        val warnings = mutableListOf<String>()

        rules.forEach { rule ->
            val result = rule.evaluate(profile)
            score += result.score
            result.warning?.let { warnings.add(it) }
        }

        // Find matching vendor adapter
        val vendorLower = profile.manufacturer.lowercase()
        adapters.forEach { adapter ->
            val adapterName = adapter.javaClass.simpleName.removeSuffix("Adapter").lowercase()
            if (vendorLower.contains(adapterName) || vendorLower.contains(adapterName.take(3))) {
                warnings.addAll(adapter.analyze(profile))
            }
        }

        return CompatibilityResult(
            level = calculateLevel(score),
            score = score.coerceIn(0, 100),
            warnings = warnings,
            blockedCapabilities = emptyList(),
        )
    }

    private fun calculateLevel(score: Int): CompatibilityLevel = when {
        score >= 80 -> CompatibilityLevel.EXCELLENT
        score >= 60 -> CompatibilityLevel.GOOD
        score >= 30 -> CompatibilityLevel.LIMITED
        else -> CompatibilityLevel.UNSUPPORTED
    }
}
