package com.astraveil.core.update

import java.io.File
import java.security.MessageDigest

/**
 * Verifies the integrity of a downloaded update package.
 *
 * AstraVeil is a root platform — a tampered update is catastrophic.
 * Every package MUST pass SHA-256 + (future) signature verification
 * before install.
 */
object UpdateVerifier {

    /** @return true iff the SHA-256 of @p file matches @p expectedHash. */
    fun verify(file: File, expectedHash: String): Boolean {
        if (!file.exists()) return false
        val actual = sha256(file)
        return actual.equals(expectedHash, ignoreCase = true)
    }

    /** Compute the SHA-256 hex string of @p file. */
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
