package com.astraveil.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SecurityManager] on the JVM.
 *
 * Constructed with `appContext = null` so [SecurityManager.isDebugBuild]
 * short-circuits to `false` and no Android `Context` is required.
 * `testOptions.unitTests.isReturnDefaultValues = true` ensures the
 * `android.util.Log` calls inside [SecurityManager.verifySignature]
 * return default values instead of throwing "not mocked".
 *
 * Coverage:
 *  - SHA-256 [SecurityManager.hash]: 64-char hex, known vector for
 *    "hello" and the empty-input vector, determinism, input sensitivity.
 *  - [SecurityManager.attestationToken] returns the documented stub.
 *  - [SecurityManager.verifySignature] rejects random signatures
 *    (the pinned Ed25519 public key will not verify forged data).
 */
class SecurityManagerTest {

    private val manager = SecurityManager(appContext = null)

    @Test
    fun `hash_produces_64_char_hex`() {
        val hash = manager.hash("hello".toByteArray())
        assertEquals(64, hash.length)
        assertTrue(
            "hash must be lowercase hex: $hash",
            hash.all { it in '0'..'9' || it in 'a'..'f' },
        )
    }

    @Test
    fun `hash_known_vector`() {
        val hash = manager.hash("hello".toByteArray())
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            hash,
        )
    }

    @Test
    fun `hash_deterministic`() {
        val a = manager.hash("deterministic-input".toByteArray())
        val b = manager.hash("deterministic-input".toByteArray())
        assertEquals(a, b)
    }

    @Test
    fun `hash_different_inputs_differ`() {
        val a = manager.hash("input-one".toByteArray())
        val b = manager.hash("input-two".toByteArray())
        assertNotEquals(a, b)
    }

    @Test
    fun `hash_empty_string`() {
        val hash = manager.hash(ByteArray(0))
        assertEquals(64, hash.length)
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash,
        )
    }

    @Test
    fun `attestationToken_returns_stub`() {
        assertEquals(
            "astraveil-attestation-stub-0000",
            manager.attestationToken(),
        )
    }

    @Test
    fun `verifySignature_with_random_signature_returns_false`() {
        val data = "important-payload".toByteArray()
        // 64-byte non-zero signature; cannot match the pinned Ed25519 key.
        val randomSignature = ByteArray(64) { ((it * 7 + 13) and 0xFF).toByte() }
        val result = manager.verifySignature(data, randomSignature)
        assertFalse("random signature must not verify against the pinned key", result)
    }
}
