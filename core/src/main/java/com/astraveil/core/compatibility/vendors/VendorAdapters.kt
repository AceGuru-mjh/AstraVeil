package com.astraveil.core.compatibility.vendors

import com.astraveil.core.device.DeviceProfile

interface VendorAdapter {
    fun analyze(profile: DeviceProfile): List<String>
}

class PixelAdapter : VendorAdapter {
    override fun analyze(profile: DeviceProfile) = emptyList<String>()
}

class SamsungAdapter : VendorAdapter {
    override fun analyze(profile: DeviceProfile) = listOf("Samsung Knox may restrict modifications")
}

class XiaomiAdapter : VendorAdapter {
    override fun analyze(profile: DeviceProfile) = listOf("MIUI/HyperOS mount restrictions possible")
}

class OppoAdapter : VendorAdapter {
    override fun analyze(profile: DeviceProfile) = listOf("ColorOS may limit background daemon")
}

class VivoAdapter : VendorAdapter {
    override fun analyze(profile: DeviceProfile) = listOf("OriginOS may restrict background processes")
}
