package com.astraveil.app.update

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.astraveil.core.logger.AstraLogger
import java.io.File
import java.security.MessageDigest

object UpdateVerifier {
    private const val TAG = "UpdateVerifier"

    data class VerificationResult(
        val checksumValid: Boolean,
        val signatureValid: Boolean,
        val expectedChecksum: String?,
        val actualChecksum: String?,
        val error: String? = null,
    ) {
        val isInstallable: Boolean get() = checksumValid && signatureValid && error == null
        fun rejectionReason(): String? = when {
            error != null -> error
            !checksumValid -> "Checksum mismatch or missing (expected=$expectedChecksum, actual=$actualChecksum)."
            !signatureValid -> "Signing certificate does not match the installed app."
            else -> null
        }
    }

    fun verify(context: Context, apkFile: File, expectedChecksum: String?): VerificationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return VerificationResult(false, false, expectedChecksum, null, "Downloaded APK is missing or empty.")
        }
        val actualChecksum = computeSha256(apkFile)
        val checksumValid = expectedChecksum != null && actualChecksum != null &&
            actualChecksum.equals(expectedChecksum.trim(), ignoreCase = true)
        val signatureValid = verifySignatureMatches(context, apkFile)
        val result = VerificationResult(checksumValid, signatureValid, expectedChecksum, actualChecksum)
        if (!result.isInstallable) AstraLogger.e(TAG, "Update rejected: ${result.rejectionReason()}")
        else AstraLogger.i(TAG, "Update verified OK (sha256=$actualChecksum)")
        return result
    }

    fun computeSha256(file: File): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192); var n = input.read(buf)
            while (n > 0) { digest.update(buf, 0, n); n = input.read(buf) }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) { null }

    fun verifySignatureMatches(context: Context, apkFile: File): Boolean {
        return try {
            val installedCerts = getInstalledCertHashes(context)
            val apkCerts = getApkCertHashes(context, apkFile)
            if (installedCerts.isEmpty() || apkCerts.isEmpty()) {
                false
            } else {
                apkCerts.any { it in installedCerts }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getInstalledCertHashes(context: Context): Set<String> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.signingCertificateHistory?.map { sha256OfCert(it) }?.toSet() ?: emptySet()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures?.map { sha256OfCert(it) }?.toSet() ?: emptySet()
        }
    }

    private fun getApkCertHashes(context: Context, apkFile: File): Set<String> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                ?.signingInfo?.apkContentsSigners?.map { sha256OfCert(it) }?.toSet() ?: emptySet()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
                ?.signatures?.map { sha256OfCert(it) }?.toSet() ?: emptySet()
        }
    }

    private fun sha256OfCert(signature: Signature): String =
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
}
