package com.astraveil.core.security

import android.os.Build
import com.astraveil.core.logger.AstraLogger
import java.security.MessageDigest

/** Lowercase hex character table used by [SecurityManager.hash]. */
private val HEX = "0123456789abcdef".toCharArray()

/**
 * Security utility surface for the AstraVeil core.
 *
 * This class will back the module sandbox policy enforcement (signature
 * verification, content hashing, debug-build detection, and attestation
 * tokens handed to root providers). Most operations are intentionally stubbed
 * today; the surface is defined so that the rest of the engine can integrate
 * against it before the real cryptographic plumbing lands.
 *
 * The class is stateless and safe to call from any thread.
 */
class SecurityManager {

    /**
     * Verify that [signature] is a valid signature over [data].
     *
     * Stub: always returns `false`. The real implementation will validate
     * against a pinned public key per provider trust policy (RSA-PSS or EC
     * over SHA-256).
     *
     * TODO: implement cryptographic signature verification.
     *
     * @param data      Original bytes that were signed.
     * @param signature Candidate signature to verify.
     * @return `true` if the signature is valid, `false` otherwise (always `false` today).
     */
    fun verifySignature(data: ByteArray, signature: ByteArray): Boolean {
        AstraLogger.d(
            "SecurityManager",
            "verifySignature called (stub) for ${data.size}B / ${signature.size}B",
        )
        return false
    }

    /**
     * Compute the SHA-256 hash of [data] and return it as a lowercase hex
     * string. The returned string is always 64 characters long.
     */
    fun hash(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(data)
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(HEX[v ushr 4])
            sb.append(HEX[v and 0x0F])
        }
        return sb.toString()
    }

    /**
     * Return `true` when the current build is a debug build (i.e.
     * [Build.DEBUG] is `true`). Used to relax certain policy checks during
     * development.
     */
    fun isDebugBuild(): Boolean = Build.DEBUG

    /**
     * Produce an attestation token that providers can use to validate the
     * caller.
     *
     * Stub: returns a fixed placeholder string. The real implementation will
     * integrate with Android KeyStore attestation to issue signed tokens that
     * the provider can verify offline.
     *
     * TODO: integrate with KeyStore attestation for real tokens.
     */
    fun attestationToken(): String = "astraveil-attestation-stub-0000"
}
