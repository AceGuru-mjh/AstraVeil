package com.astraveil.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ProviderCapability] — the v3 capability tokens a
 * [RootProvider] can advertise.
 *
 * The enum is a stable contract: AstraVeil resolves capability requests by
 * enum value, so the set of values and their `valueOf` lookups MUST stay
 * stable across releases. These tests pin that contract.
 */
class ProviderCapabilityTest {

    @Test
    fun enum_has_6_values() {
        val values = ProviderCapability.values()

        assertEquals(6, values.size)
        assertTrue(ProviderCapability.ROOT_EXECUTION in values)
        assertTrue(ProviderCapability.MOUNT_NAMESPACE in values)
        assertTrue(ProviderCapability.OVERLAY_FS in values)
        assertTrue(ProviderCapability.SYSTEM_PROPERTY in values)
        assertTrue(ProviderCapability.BOOT_PATCH in values)
        assertTrue(ProviderCapability.SELINUX_CONTROL in values)
    }

    @Test
    fun valueOf_by_name() {
        assertEquals(
            ProviderCapability.ROOT_EXECUTION,
            ProviderCapability.valueOf("ROOT_EXECUTION")
        )
        assertEquals(
            ProviderCapability.MOUNT_NAMESPACE,
            ProviderCapability.valueOf("MOUNT_NAMESPACE")
        )
        assertEquals(
            ProviderCapability.OVERLAY_FS,
            ProviderCapability.valueOf("OVERLAY_FS")
        )
        assertEquals(
            ProviderCapability.SYSTEM_PROPERTY,
            ProviderCapability.valueOf("SYSTEM_PROPERTY")
        )
        assertEquals(
            ProviderCapability.BOOT_PATCH,
            ProviderCapability.valueOf("BOOT_PATCH")
        )
        assertEquals(
            ProviderCapability.SELINUX_CONTROL,
            ProviderCapability.valueOf("SELINUX_CONTROL")
        )
    }

    @Test
    fun all_values_are_distinct() {
        val values = ProviderCapability.values()
        val names = values.map { it.name }

        assertEquals("Enum names must be unique", names.size, names.toSet().size)
        assertEquals("Enum ordinals must be unique", values.size, values.map { it.ordinal }.toSet().size)

        // Each value must be referentially distinct from every other.
        for (a in values) {
            for (b in values) {
                if (a === b) {
                    assertTrue("identity mismatch for ${a.name}", a == b)
                } else {
                    assertTrue(
                        "distinct enum constants must not be equal: ${a.name} vs ${b.name}",
                        a != b
                    )
                }
            }
        }
    }
}
