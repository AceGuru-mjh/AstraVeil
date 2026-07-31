package com.astraveil.modules.security

import com.astraveil.modules.registry.SignatureStatus
import java.io.File
import java.security.MessageDigest

data class TrustReport(
    val sourceHash: String?,
    val signatureStatus: SignatureStatus,
    val manifestValid: Boolean,
    val apiVersionSupported: Boolean,
    val warnings: List<String> = emptyList(),
) {
    val isInstallable: Boolean
        get() = sourceHash != null && manifestValid && apiVersionSupported && signatureStatus != SignatureStatus.INVALID
    fun isInstallable(strict: Boolean): Boolean {
        if (!isInstallable) return false
        if (strict && signatureStatus != SignatureStatus.VERIFIED) return false
        return true
    }
    fun rejectionReason(strict: Boolean): String? {
        if (sourceHash == null) return "Integrity hash could not be computed."
        if (!manifestValid) return "Manifest is invalid or missing."
        if (!apiVersionSupported) return "Module API version is not supported."
        if (signatureStatus == SignatureStatus.INVALID) return "Signature verification failed."
        if (strict && signatureStatus != SignatureStatus.VERIFIED) return "Strict mode requires a verified signature (got $signatureStatus)."
        return null
    }
}

object TrustGate {
    private val SUPPORTED_API_VERSIONS = setOf(1, 2, 3)

    fun evaluate(stagedFile: File, manifestValid: Boolean, apiVersion: Int, signatureStatus: SignatureStatus): TrustReport {
        val warnings = buildList {
            if (signatureStatus == SignatureStatus.UNSIGNED) add("Module is not signed.")
            if (signatureStatus == SignatureStatus.UNKNOWN) add("Signature could not be verified against a trusted key.")
        }
        return TrustReport(sourceHash = sha256OrNull(stagedFile), signatureStatus = signatureStatus, manifestValid = manifestValid, apiVersionSupported = apiVersion in SUPPORTED_API_VERSIONS, warnings = warnings)
    }

    fun requireInstallable(report: TrustReport, strict: Boolean) {
        if (!report.isInstallable(strict)) throw SecurityException("Module rejected by trust gate: ${report.rejectionReason(strict)}")
    }

    fun verifyHash(file: File, expectedHash: String): Boolean {
        val actual = sha256OrNull(file) ?: return false
        return actual.equals(expectedHash, ignoreCase = true)
    }

    private fun sha256OrNull(file: File): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192); var n = input.read(buf)
            while (n > 0) { digest.update(buf, 0, n); n = input.read(buf) }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { null }
}
