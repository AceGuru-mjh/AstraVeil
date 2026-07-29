package com.astraveil.core.modules.security

import java.io.InputStream
import java.security.MessageDigest

/**
 * Stateless SHA-256 hasher for `.avm` package integrity checking (PR18.3).
 *
 * Why SHA-256 lives in the Trust Pipeline:
 *  - Root modules are the highest-risk code path on the device. The
 *    #1 threat is not "too many permissions" but **package
 *    substitution** — a legit module replaced with a trojaned build.
 *  - Surfacing the package fingerprint in the install dialog lets the
 *    user (and future signature verification) pin a specific build.
 *
 * Pure JVM (`java.security.MessageDigest`) — no Android dependency —
 * so the daemon and CLI can reuse the same hasher.
 *
 * Thread-safe: each call allocates its own [MessageDigest] instance.
 */
object HashCalculator {

    private const val SHA_256 = "SHA-256"
    private val HEX = "0123456789abcdef".toCharArray()

    /** Read [stream] to EOF and return the lowercase hex SHA-256 digest. */
    fun sha256(stream: InputStream): String {
        val digest = MessageDigest.getInstance(SHA_256)
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return toHex(digest.digest())
    }

    private fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        var i = 0
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            out[i++] = HEX[v ushr 4]
            out[i++] = HEX[v and 0x0F]
        }
        return String(out)
    }
}
