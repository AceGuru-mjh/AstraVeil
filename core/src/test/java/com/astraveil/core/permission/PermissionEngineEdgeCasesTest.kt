package com.astraveil.core.permission

import com.astraveil.core.event.EventBus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Edge-case unit tests for [PermissionEngine].
 *
 * This file is intentionally ADDITIVE — it complements the existing
 * `PermissionEngineTest` (which covers the happy path: grant/revoke,
 * dangerous-approval gating, the v3 `evaluate` decision tree, and basic
 * dump/load round-trips). The 10 cases below probe the no-op, isolation,
 * overload, and idempotency edges that the original suite does not cover.
 *
 * `PermissionEngine` holds its grant state internally per instance, so each
 * test gets a fresh engine via [setup]. The shared [EventBus] singleton is
 * used purely for emission — no test subscribes, so emitted events are
 * simply dropped on the floor by the bus's DROP_OLDEST buffer policy.
 */
class PermissionEngineEdgeCasesTest {

    private lateinit var engine: PermissionEngine

    @Before
    fun setup() {
        engine = PermissionEngine(EventBus)
    }

    @Test
    fun revoke_nonexistent_is_noop() {
        // Revoke on a module the engine has never seen, plus an unknown
        // permission, must not throw and must leave the engine empty.
        engine.revoke("does.not.exist", "filesystem")
        assertTrue(engine.dumpPermissions().isEmpty())
    }

    @Test
    fun revokeAll_on_unknown_module_is_noop() {
        // revokeAll on an unknown module id is a silent no-op.
        engine.revokeAll("never.granted")
        assertTrue(engine.dumpPermissions().isEmpty())
        // And it must not affect a real module that was previously loaded.
        engine.grant("mod.a", "filesystem")
        engine.revokeAll("never.granted")
        assertTrue(engine.canExecute("mod.a", "filesystem"))
    }

    @Test
    fun canExecute_without_grant_returns_false() {
        // A module id with no prior grants must report `canExecute == false`
        // for any permission token.
        assertFalse(engine.canExecute("mod.empty", "filesystem"))
        assertFalse(engine.canExecute("mod.empty", "network"))
    }

    @Test
    fun multiple_modules_isolated() {
        // Grants to one module id must NOT bleed into another module id.
        engine.grant("mod.a", "filesystem")
        engine.grant("mod.b", "network")

        assertTrue(engine.canExecute("mod.a", "filesystem"))
        assertTrue(engine.canExecute("mod.b", "network"))

        // Cross-checks fail: mod.a does not hold network, mod.b does not
        // hold filesystem.
        assertFalse(engine.canExecute("mod.a", "network"))
        assertFalse(engine.canExecute("mod.b", "filesystem"))
    }

    @Test
    fun loadPermissions_clears_previous_state() {
        // Seed the engine with a real grant, then load an EMPTY map. The
        // previous grants must be wiped — `loadPermissions` is a REPLACE,
        // not a MERGE.
        engine.grant("mod.a", "filesystem")
        engine.grant("mod.b", "network")

        engine.loadPermissions(emptyMap())

        assertFalse(engine.canExecute("mod.a", "filesystem"))
        assertFalse(engine.canExecute("mod.b", "network"))
        assertTrue(engine.dumpPermissions().isEmpty())
    }

    @Test
    fun request_builtin_overload_uses_builtin_module_id() {
        // The single-arg `request(permission)` overload attributes the
        // grant to PermissionEngine.BUILTIN_MODULE_ID. Subsequent calls to
        // the explicit two-arg `canExecute(BUILTIN_MODULE_ID, permission)`
        // must observe the grant.
        assertTrue(engine.request("filesystem"))
        assertTrue(
            engine.canExecute(PermissionEngine.BUILTIN_MODULE_ID, "filesystem"),
        )
    }

    @Test
    fun has_builtin_overload_reflects_builtin_grants() {
        // `has(permission)` is the read-side counterpart of the single-arg
        // `request` overload: it checks the builtin module id only.
        assertFalse(engine.has("filesystem"))
        engine.grant(PermissionEngine.BUILTIN_MODULE_ID, "filesystem")
        assertTrue(engine.has("filesystem"))
    }

    @Test
    fun list_returns_empty_for_unknown_module() {
        // `list(moduleId)` returns a defensive empty PermissionSet for any
        // module id the engine has never granted to — never `null`.
        val unknown = engine.list("never.granted")
        assertEquals(emptySet<String>(), unknown.permissions)
        assertFalse("filesystem" in unknown)
    }

    @Test
    fun grant_idempotent() {
        // Granting the same permission twice must not duplicate the entry
        // nor break `canExecute`. The dump should contain exactly one
        // entry for the permission.
        engine.grant("mod.a", "filesystem")
        engine.grant("mod.a", "filesystem")

        assertTrue(engine.canExecute("mod.a", "filesystem"))
        val dumped = engine.dumpPermissions()["mod.a"]
        assertEquals(setOf("filesystem"), dumped)
    }

    @Test
    fun dumpPermissions_empty_when_no_grants() {
        // A fresh engine has granted nothing — the dump must be an empty
        // map, not null.
        assertEquals(emptyMap<String, Set<String>>(), engine.dumpPermissions())
    }
}
