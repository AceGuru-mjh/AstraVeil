package com.astraveil.core.capability

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CapabilityEngine's pure-logic paths.
 * Device-dependent probes (procfs, sysfs) are tested via instrumented tests.
 */
class CapabilityEngineTest {

    @Test
    fun `suPaths covers standard locations`() {
        val engine = CapabilityEngine()
        // Access via reflection since suPaths is private
        val field = CapabilityEngine::class.java.getDeclaredField("suPaths")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val paths = field.get(engine) as List<String>

        assertTrue(paths.contains("/system/bin/su"))
        assertTrue(paths.contains("/system/xbin/su"))
        assertTrue(paths.contains("/sbin/su"))
        assertTrue(paths.size >= 5)
    }

    @Test
    fun `toJson produces valid JSON with required fields`() {
        val engine = CapabilityEngine()
        val info = CapabilityInfo(
            androidVersion = "14",
            apiLevel = 34,
            abi = "arm64-v8a",
            abis = listOf("arm64-v8a", "armeabi-v7a"),
            kernelVersion = "6.1.0",
            selinuxStatus = SelinuxStatus.ENFORCING,
            selinuxMode = "1",
            rootAvailable = true,
            rootProvider = "magisk",
            mountCapability = true,
            overlayFsCapability = true,
            namespaceCapability = true,
            pidNamespace = true,
            deviceModel = "Pixel 8",
            deviceManufacturer = "Google",
            deviceBrand = "google",
            fingerprint = "google/shiba/shiba:14/UP1A.231005.007/10754064:user/release-keys",
        )
        val json = engine.toJson(info)

        assertTrue(json.contains("\"androidVersion\""))
        assertTrue(json.contains("\"apiLevel\""))
        assertTrue(json.contains("\"rootAvailable\""))
        assertTrue(json.contains("\"overlayFsCapability\""))
        assertTrue(json.contains("Pixel 8"))
    }

    @Test
    fun `CapabilityInfo empty has safe defaults`() {
        val empty = CapabilityInfo.empty()
        assertFalse(empty.rootAvailable)
        assertEquals("none", empty.rootProvider)
        assertEquals(0, empty.apiLevel)
    }
}
