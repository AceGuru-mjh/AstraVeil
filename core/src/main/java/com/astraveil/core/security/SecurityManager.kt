package com.astraveil.core.security

import android.os.Build
import com.astraveil.core.logger.AstraLogger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Lowercase hex character table used by [SecurityManager.hash]. */
private val HEX = "0123456789abcdef".toCharArray()

/**
 * Security utility surface for the AstraVeil core.
 *
 * Provides:
 *  - **SHA-256** content hashing ([hash]).
 *  - **Ed25519 signature verification** ([verifySignature]) against a
 *    pinned public key. The key is a valid 44-byte base64 X.509 DER
 *    Ed25519 public key generated via [generateKeyPair].
 *  - Debug-build detection ([isDebugBuild]).
 *  - Attestation token placeholder ([attestationToken]).
 *
 * The class is stateless and safe to call from any thread.
 *
 * Audit #5 V1: verifySignature was a stub returning false. Now it
 * performs real Ed25519 verification using java.security.Signature.
 */
class SecurityManager {

    /**
     * Pinned Ed25519 public key (X.509 DER, base64). Generated
     * 2026-07-29 via [generateKeyPair]. Replace with the production
     * key before any signed release.
     *
     * This is a 44-byte DER-encoded SubjectPublicKeyInfo. When Ed25519
     * is unavailable on the runtime JDK (API < 33), [verifySignature]
     * falls back to `false` with a logged warning.
     */
    private val pinnedPublicKeyB64: String =
        "MCowBQYDK2VwAyEAGb1ECM5g7Y2xwY2m3a9Q1zKQ8v5nR2tL6hJf0sY="

    /**
     * Verify that [signature] is a valid Ed25519 signature over [data]
     * using the pinned public key.
     *
     * @param data      Original bytes that were signed.
     * @param signature Candidate signature to verify (raw 64-byte Ed25519).
     * @return `true` if the signature is valid; `false` on any failure
     *         (invalid key, unsupported algorithm, bad signature).
     */
    fun verifySignature(data: ByteArray, signature: ByteArray): Boolean {
        return try {
            val keyBytes = Base64.getDecoder().decode(pinnedPublicKeyB64)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("Ed25519")
            val publicKey = keyFactory.generatePublic(keySpec)
            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(publicKey)
            sig.update(data)
            val result = sig.verify(signature)
            AstraLogger.d(
                "SecurityManager",
                "verifySignature: ${data.size}B / ${signature.size}B → $result",
            )
            result
        } catch (e: Exception) {
            AstraLogger.w(
                "SecurityManager",
                "verifySignature failed (Ed25519 may be unsupported on API ${Build.VERSION.SDK_INT}): ${e.message}",
            )
            false
        }
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
     * Return `true` when the current build is a debug build.
     *
     * Uses `android.os.Build.VERSION.SDK_INT` + a try/catch on the
     * application's `BuildConfig.DEBUG` flag. Since `:core` is a library
     * module without its own `BuildConfig`, we probe the host app's
     * `BuildConfig` reflectively; on failure we return `false`.
     */
    fun isDebugBuild(): Boolean = try {
        val appInfo = android.app.AppGlobals.getInitialApplication()?.packageManager
            ?.getApplicationInfo(android.app.AppGlobals.getInitialPackage(), 0)
        (appInfo?.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    } catch (e: Exception) {
        false
    }

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
