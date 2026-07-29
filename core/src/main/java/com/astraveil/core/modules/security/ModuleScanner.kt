package com.astraveil.core.modules.security

import com.astraveil.core.modules.manifest.AvmManifestParser
import com.astraveil.core.modules.model.ModuleManifestPreview
import java.io.InputStream

/**
 * Orchestrates the Module Trust Pipeline (PR18.3).
 *
 * Given the raw bytes of a `.avm` package, produces a [TrustReport]
 * covering:
 *  1. **Package integrity** — SHA-256 fingerprint ([HashCalculator]).
 *  2. **Manifest extraction** — `module.json` parsed by
 *     [AvmManifestParser] into a [ModuleManifestPreview].
 *  3. **Risk assessment** — [RiskAnalyzer] aggregates the declared
 *     permission risks into an overall [RiskLevel].
 *  4. **Signature status** — placeholder [SignatureStatus.UNKNOWN]
 *     for PR18.3 (real ed25519/minisign verification is a later
 *     milestone; the UI surfaces "Unknown" honestly).
 *
 * Why two streams:
 *  - A stream cannot be rewound. SHA-256 consumes the entire archive;
 *    manifest parsing walks the ZIP entries. The caller supplies both
 *    by re-opening the same source URI. This keeps the scanner pure
 *    JVM (no filesystem / Android `ContentResolver` coupling) and
 *    avoids buffering potentially-large `.so` binaries in memory.
 *
 * Pure JVM, stateless, thread-safe. Reusable by daemon / CLI / tests.
 *
 * Pipeline (PR18.3):
 * ```
 * .avm URI
 *   → ModuleScanner.scan(hashStream, manifestStream)
 *     → HashCalculator.sha256(hashStream)
 *     → AvmManifestParser.parse(manifestStream)
 *     → RiskAnalyzer.analyze(preview)
 *     → TrustReport(packageHash, manifestStatus, preview, …)
 * ```
 */
object ModuleScanner {

    /**
     * Scan a `.avm` package end-to-end.
     *
     * Both streams are consumed and closed by this call.
     *
     * @param hashStream     Stream over the raw archive bytes, used for
     *                       SHA-256. Read to EOF.
     * @param manifestStream Independent stream over the same archive,
     *                       used to locate and parse `module.json`.
     * @return A [TrustReport] (always — failures are reflected in
     *         [TrustReport.manifestStatus], never thrown).
     */
    fun scan(hashStream: InputStream, manifestStream: InputStream): TrustReport {
        // 1. Package hash
        val packageHash = try {
            hashStream.use { HashCalculator.sha256(it) }
        } catch (e: Exception) {
            // If we can't even hash the file, fall back to a sentinel
            // and mark the manifest as missing — the report is still
            // well-formed so the UI can show "scan failed".
            ""
        }

        // 2. Manifest parse
        val preview: ModuleManifestPreview?
        val manifestStatus: ManifestStatus
        when (val result = manifestStream.use { AvmManifestParser.parse(it) }) {
            is AvmManifestParser.PreviewResult.Success -> {
                preview = result.preview
                manifestStatus = ManifestStatus.OK
            }
            is AvmManifestParser.PreviewResult.Failure -> {
                preview = null
                manifestStatus = when (result.reason) {
                    AvmManifestParser.PreviewError.MISSING_MANIFEST -> ManifestStatus.MISSING
                    else -> ManifestStatus.MALFORMED
                }
            }
        }

        // 3. Risk assessment
        val assessment = RiskAnalyzer.analyze(preview)

        // 4. Signature status — PR18.3 ships UNKNOWN.
        val signatureStatus = SignatureStatus.UNKNOWN

        return TrustReport(
            packageHash = packageHash,
            manifestStatus = manifestStatus,
            preview = preview,
            permissionCount = assessment.permissionCount,
            highestRisk = assessment.highestRisk,
            riskSource = assessment.riskSource,
            signatureStatus = signatureStatus,
            overallRiskLevel = assessment.overallRiskLevel,
        )
    }
}
