package com.astraveil.modules.security

import com.astraveil.modules.registry.SignatureStatus
import java.io.File
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.ZipFile

enum class TrustLevel { OFFICIAL, TRUSTED_DEVELOPER, UNKNOWN_DEVELOPER, UNSIGNED, INVALID }

data class SignatureVerification(
    val cryptoStatus: SignatureStatus,
    val trustLevel: TrustLevel,
    val signerFingerprint: String?,
    val signerName: String?,
    val reason: String?,
)

object ModuleSignatureVerifier {
    private const val MF_PATH = "META-INF/ASTRAVEIL.MF"
    private const val SIG_PATH = "META-INF/ASTRAVEIL.SIG"
    private const val CERT_PATH = "META-INF/ASTRAVEIL.CERT"

    fun verify(avmFile: File, officialPublicKeyB64: String, trustedKeys: Set<String>): SignatureVerification {
        return try {
            ZipFile(avmFile).use { zip ->
                val mfEntry = zip.getEntry(MF_PATH) ?: return unsigned()
                val sigEntry = zip.getEntry(SIG_PATH) ?: return unsigned()
                val certEntry = zip.getEntry(CERT_PATH) ?: return unsigned()
                val mfBytes = zip.getInputStream(mfEntry).readBytes()
                val sigBytes = zip.getInputStream(sigEntry).readBytes()
                val certB64 = zip.getInputStream(certEntry).readBytes().toString(Charsets.UTF_8).trim()
                val certBytes = Base64.getDecoder().decode(certB64)
                val publicKey = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(certBytes))
                val verifier = Signature.getInstance("Ed25519")
                verifier.initVerify(publicKey); verifier.update(mfBytes)
                if (!verifier.verify(sigBytes)) return invalid("signature does not verify")
                val manifest = parseManifest(mfBytes.toString(Charsets.UTF_8))
                val digest = MessageDigest.getInstance("SHA-256")
                for ((path, expectedHash) in manifest.files) {
                    val entry = zip.getEntry(path) ?: return invalid("manifest lists missing file: $path")
                    val actual = digest.digest(zip.getInputStream(entry).readBytes()).joinToString("") { "%02x".format(it) }
                    if (!actual.equals(expectedHash, ignoreCase = true)) return invalid("content tampered: $path")
                }
                val trustLevel = when {
                    certB64 == officialPublicKeyB64.trim() -> TrustLevel.OFFICIAL
                    certB64 in trustedKeys -> TrustLevel.TRUSTED_DEVELOPER
                    else -> TrustLevel.UNKNOWN_DEVELOPER
                }
                SignatureVerification(SignatureStatus.VERIFIED, trustLevel, fingerprint(certBytes), manifest.signer, null)
            }
        } catch (e: Exception) {
            SignatureVerification(SignatureStatus.INVALID, TrustLevel.INVALID, null, null, "verification error: ${e.message}")
        }
    }

    private fun unsigned() = SignatureVerification(SignatureStatus.UNSIGNED, TrustLevel.UNSIGNED, null, null, "no signature block")
    private fun invalid(reason: String) = SignatureVerification(SignatureStatus.INVALID, TrustLevel.INVALID, null, null, reason)

    /**
     * Public accessor for the "no signature block" verdict. Used by
     * [com.astraveil.modules.ModuleManager] as a safe fallback when the
     * structured verifier throws, so the install record still carries a
     * well-formed [TrustLevel] (`UNSIGNED`) rather than crashing.
     */
    fun unsignedVerification(): SignatureVerification = unsigned()
    private fun fingerprint(certBytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(certBytes).joinToString(":") { "%02x".format(it) }

    private data class Manifest(val signer: String?, val files: Map<String, String>)
    private fun parseManifest(text: String): Manifest {
        var signer: String? = null; val files = LinkedHashMap<String, String>(); var currentName: String? = null
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            when {
                line.startsWith("Signer:") -> signer = line.removePrefix("Signer:").trim()
                line.startsWith("Name:") -> currentName = line.removePrefix("Name:").trim()
                line.startsWith("SHA-256:") -> { currentName?.let { files[it] = line.removePrefix("SHA-256:").trim() }; currentName = null }
            }
        }
        return Manifest(signer, files)
    }
}
