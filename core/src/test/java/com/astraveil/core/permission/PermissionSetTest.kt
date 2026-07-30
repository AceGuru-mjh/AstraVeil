package com.astraveil.core.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [PermissionSet] data class.
 *
 * `PermissionSet` is the immutable, serializable collection of permission
 * tokens held by a single module. It is intentionally minimal: every
 * mutation returns a brand-new instance so callers can share references
 * freely across coroutine boundaries.
 *
 * These tests exercise the public surface of [PermissionSet] directly,
 * without going through [PermissionEngine]. The engine has its own
 * dedicated suites (`PermissionEngineTest` and
 * `PermissionEngineEdgeCasesTest`); this file isolates the data class
 * semantics: grant/revoke round-trips, `contains`/`has` lookups, default
 * construction, and accumulation across multiple grants.
 */
class PermissionSetTest {

    @Test
    fun grant_adds_permission() {
        val original = PermissionSet()
        val updated = original.grant("filesystem")

        assertFalse("filesystem" in original)
        assertTrue("filesystem" in updated)
    }

    @Test
    fun revoke_removes_permission() {
        val set = PermissionSet(setOf("filesystem", "network"))
        val updated = set.revoke("filesystem")

        assertFalse("filesystem" in updated)
        assertTrue("network" in updated)
    }

    @Test
    fun revoke_nonexistent_is_noop() {
        // Removing a permission that was never present must yield an
        // equivalent set rather than throwing.
        val set = PermissionSet(setOf("filesystem"))
        val updated = set.revoke("never.granted")

        assertEquals(set, updated)
        assertTrue("filesystem" in updated)
    }

    @Test
    fun contains_returns_true_after_grant() {
        val set = PermissionSet().grant("mount")

        assertTrue("mount" in set)
        assertFalse("network" in set)
    }

    @Test
    fun has_returns_true_after_grant() {
        // `has(name)` is the named form of `contains(name)`.
        val set = PermissionSet().grant("su")

        assertTrue(set.has("su"))
        assertFalse(set.has("kernel_hook"))
    }

    @Test
    fun empty_set_has_no_permissions() {
        val empty = PermissionSet(emptySet())

        assertTrue(empty.permissions.isEmpty())
        assertFalse("anything" in empty)
        assertFalse(empty.has("anything"))
    }

    @Test
    fun grant_multiple_accumulates() {
        // Each `grant` returns a new set with the previous contents plus
        // the new token. Chaining grants accumulates without loss.
        val set = PermissionSet()
            .grant("filesystem")
            .grant("network")
            .grant("property")

        assertEquals(setOf("filesystem", "network", "property"), set.permissions)
        assertTrue("filesystem" in set)
        assertTrue("network" in set)
        assertTrue("property" in set)
    }

    @Test
    fun default_constructor_empty() {
        // The no-arg constructor must default to an empty permission set,
        // not null and not a placeholder sentinel.
        val set = PermissionSet()

        assertEquals(emptySet<String>(), set.permissions)
        assertTrue(set.permissions.isEmpty())
    }
}
