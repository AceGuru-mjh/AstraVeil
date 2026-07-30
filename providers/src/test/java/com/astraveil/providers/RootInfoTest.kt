package com.astraveil.providers

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [RootInfo] — the value type produced by every [RootProvider]
 * when AstraVeil probes the active root backend.
 *
 * Covers the `none()` sentinel, `data class` equality / `copy` semantics, and
 * kotlinx.serialization round-tripping. All tests run on plain JVM with no
 * Android framework dependency.
 */
class RootInfoTest {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun none_returns_safe_defaults() {
        val none = RootInfo.none()

        assertEquals("none", none.providerName)
        assertEquals("None", none.displayName)
        assertEquals("unknown", none.version)
        assertEquals(0, none.versionCode)
        assertFalse("suAvailable must be false on the none() sentinel", none.suAvailable)
        assertEquals("", none.modulePath)
        assertTrue("supportedFeatures must be empty on the none() sentinel", none.supportedFeatures.isEmpty())
        assertFalse("detected must be false on the none() sentinel", none.detected)
    }

    @Test
    fun rootInfo_equality() {
        val a = RootInfo(
            providerName = "magisk",
            displayName = "Magisk",
            version = "26.4",
            versionCode = 26400,
            suAvailable = true,
            modulePath = "/data/adb/modules",
            supportedFeatures = setOf("mount", "namespace"),
            detected = true
        )
        val b = RootInfo(
            providerName = "magisk",
            displayName = "Magisk",
            version = "26.4",
            versionCode = 26400,
            suAvailable = true,
            modulePath = "/data/adb/modules",
            supportedFeatures = setOf("mount", "namespace"),
            detected = true
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun rootInfo_copy() {
        val base = RootInfo(
            providerName = "kernelsu",
            displayName = "KernelSU",
            version = "0.7.0"
        )
        val bumped = base.copy(version = "0.8.0")

        assertNotEquals(base, bumped)
        assertEquals("0.7.0", base.version)
        assertEquals("0.8.0", bumped.version)
        // Unchanged fields are preserved by copy().
        assertEquals(base.providerName, bumped.providerName)
        assertEquals(base.displayName, bumped.displayName)
    }

    @Test
    fun rootInfo_serialization_round_trip() {
        val original = RootInfo(
            providerName = "apatch",
            displayName = "APatch",
            version = "0.10.5",
            versionCode = 105,
            suAvailable = true,
            modulePath = "/data/ap/modules",
            supportedFeatures = setOf("mount", "namespace", "hook"),
            detected = true
        )

        val encoded = json.encodeToString(RootInfo.serializer(), original)
        val decoded = json.decodeFromString(RootInfo.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun default_versionCode_is_zero() {
        val info = RootInfo(
            providerName = "astraroot",
            displayName = "AstraRoot",
            version = "1.0.0"
        )

        assertEquals(0, info.versionCode)
    }

    @Test
    fun default_supportedFeatures_is_empty() {
        val info = RootInfo(
            providerName = "astraroot",
            displayName = "AstraRoot",
            version = "1.0.0"
        )

        assertTrue(info.supportedFeatures.isEmpty())
    }

    @Test
    fun default_modulePath_is_empty() {
        val info = RootInfo(
            providerName = "astraroot",
            displayName = "AstraRoot",
            version = "1.0.0"
        )

        assertEquals("", info.modulePath)
    }
}
