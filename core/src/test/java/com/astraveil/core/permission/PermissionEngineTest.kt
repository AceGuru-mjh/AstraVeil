package com.astraveil.core.permission

import com.astraveil.core.event.EventBus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PermissionEngineTest {

    private lateinit var engine: PermissionEngine

    @Before
    fun setup() {
        engine = PermissionEngine(EventBus)
    }

    @Test
    fun `grant and check permission`() {
        engine.grant("mod.a", "filesystem")
        assertTrue(engine.canExecute("mod.a", "filesystem"))
        assertFalse(engine.canExecute("mod.a", "su"))
    }

    @Test
    fun `revoke removes permission`() {
        engine.grant("mod.a", "mount")
        assertTrue(engine.canExecute("mod.a", "mount"))
        engine.revoke("mod.a", "mount")
        assertFalse(engine.canExecute("mod.a", "mount"))
    }

    @Test
    fun `dangerous permission denied without approval`() {
        engine.setDangerousApproval(false)
        assertFalse(engine.request("mod.a", "su"))
        assertFalse(engine.request("mod.a", "mount"))
        assertFalse(engine.request("mod.a", "kernel_hook"))
        assertFalse(engine.request("mod.a", "namespace"))
    }

    @Test
    fun `dangerous permission granted with approval`() {
        engine.setDangerousApproval(true)
        assertTrue(engine.request("mod.a", "su"))
        assertTrue(engine.canExecute("mod.a", "su"))
    }

    @Test
    fun `non-dangerous permission granted freely`() {
        assertTrue(engine.request("mod.a", "filesystem"))
        assertTrue(engine.canExecute("mod.a", "filesystem"))
    }

    @Test
    fun `evaluate returns ALLOW for granted permission`() {
        engine.grant("mod.a", "overlayfs")
        val decision = engine.evaluate(
            PermissionRequest("mod.a", "overlayfs", "test")
        )
        assertEquals(PermissionDecision.ALLOW, decision)
    }

    @Test
    fun `evaluate returns DENY for dangerous without approval`() {
        engine.setDangerousApproval(false)
        val decision = engine.evaluate(
            PermissionRequest("mod.a", "su", "test")
        )
        assertEquals(PermissionDecision.DENY, decision)
    }

    @Test
    fun `evaluate returns REQUIRE_APPROVAL for ungranted non-dangerous`() {
        val decision = engine.evaluate(
            PermissionRequest("mod.a", "filesystem", "test")
        )
        assertEquals(PermissionDecision.REQUIRE_APPROVAL, decision)
    }

    @Test
    fun `revokeAll clears all permissions for module`() {
        engine.grant("mod.a", "filesystem")
        engine.grant("mod.a", "network")
        engine.grant("mod.a", "property")
        engine.revokeAll("mod.a")
        assertFalse(engine.canExecute("mod.a", "filesystem"))
        assertFalse(engine.canExecute("mod.a", "network"))
        assertFalse(engine.canExecute("mod.a", "property"))
    }

    @Test
    fun `dumpPermissions returns snapshot`() {
        engine.grant("mod.a", "filesystem")
        engine.grant("mod.b", "su")
        val dump = engine.dumpPermissions()
        assertEquals(setOf("filesystem"), dump["mod.a"])
        assertEquals(setOf("su"), dump["mod.b"])
    }

    @Test
    fun `loadPermissions replaces state`() {
        engine.grant("mod.a", "filesystem")
        engine.loadPermissions(mapOf("mod.x" to setOf("mount", "su")))
        assertFalse(engine.canExecute("mod.a", "filesystem"))
        assertTrue(engine.canExecute("mod.x", "mount"))
        assertTrue(engine.canExecute("mod.x", "su"))
    }
}
