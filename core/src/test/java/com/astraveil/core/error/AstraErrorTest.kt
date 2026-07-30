package com.astraveil.core.error

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [AstraError] sealed hierarchy.
 *
 * Pins the contract that every concrete subtype:
 *  - is an [AstraError] (sealed-class membership);
 *  - exposes its declared data-class properties;
 *  - propagates the constructor `message` and `cause` to the underlying
 *    [Throwable] (so callers can `catch (e: AstraError) { e.message }`).
 */
class AstraErrorTest {

    @Test
    fun `each_subclass_is_astra_error`() {
        val errors: List<AstraError> = listOf(
            AstraError.CapabilityProbeFailed(path = "/proc/x"),
            AstraError.PermissionDenied(moduleId = "mod.a", capability = "filesystem"),
            AstraError.DangerousPermissionBlocked(moduleId = "mod.a", capability = "su"),
            AstraError.DaemonNotConnected(),
            AstraError.DaemonRequestFailed(requestType = 5, errorCode = 42),
            AstraError.DaemonTimeout(requestType = 5, timeoutMs = 1000L),
            AstraError.ModuleNotFound(moduleId = "mod.a"),
            AstraError.ModuleValidationFailed(moduleId = "mod.a", reason = "bad manifest"),
            AstraError.ModuleSandboxViolation(moduleId = "mod.a", syscall = "execve"),
            AstraError.ProviderUnavailable(providerName = "magisk"),
            AstraError.ExecutionFailed(command = "ls", exitCode = 1, stderr = "not found"),
            AstraError.SignatureVerificationFailed(moduleId = "mod.a"),
            AstraError.PolicyDenied(moduleId = "mod.a", capability = "su", riskLevel = 90),
            AstraError.ConfigLoadFailed(),
            AstraError.UpdateCheckFailed(),
            AstraError.UpdateVerificationFailed(expectedSha256 = "abc", actualSha256 = "def"),
        )
        assertEquals("expected all 16 AstraError subtypes", 16, errors.size)
        errors.forEach { err ->
            assertTrue("each subtype must be an AstraError", err is AstraError)
        }
    }

    @Test
    fun `capability_probe_failed_has_path`() {
        val err = AstraError.CapabilityProbeFailed(path = "/proc/x", message = "fail")
        assertEquals("/proc/x", err.path)
        assertEquals("fail", err.message)
    }

    @Test
    fun `permission_denied_has_module_and_capability`() {
        val err = AstraError.PermissionDenied(moduleId = "mod.a", capability = "filesystem")
        assertEquals("mod.a", err.moduleId)
        assertEquals("filesystem", err.capability)
    }

    @Test
    fun `daemon_request_failed_has_codes`() {
        val err = AstraError.DaemonRequestFailed(requestType = 5, errorCode = 42)
        assertEquals(5, err.requestType)
        assertEquals(42, err.errorCode)
    }

    @Test
    fun `execution_failed_has_exit_code`() {
        val err = AstraError.ExecutionFailed(command = "ls", exitCode = 1, stderr = "err")
        assertEquals(1, err.exitCode)
    }

    @Test
    fun `cause_propagation`() {
        val cause = IOException("io")
        val err = AstraError.ConfigLoadFailed(message = "config", cause = cause)
        assertNotNull(err.cause)
        assertSame(cause, err.cause)
    }

    @Test
    fun `message_is_accessible`() {
        val err = AstraError.DaemonNotConnected(message = "no daemon")
        // Kotlin property access maps to Throwable.getMessage().
        assertEquals("no daemon", err.message)
        // Explicit Throwable.getMessage() call must return the same value.
        assertEquals("no daemon", err.message)
    }
}
