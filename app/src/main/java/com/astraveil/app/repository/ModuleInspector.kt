package com.astraveil.app.repository

import android.content.Context
import android.net.Uri
import com.astraveil.core.modules.security.ModuleScanner
import com.astraveil.core.modules.security.TrustReport

/**
 * High-level `:app` facade over the `:core` Module Trust Pipeline (PR18.3).
 *
 * Owns the Android `ContentResolver` interaction: opens TWO independent
 * streams over the picked `.avm` URI (one for SHA-256, one for manifest
 * parsing — a stream cannot be rewound) and delegates to
 * [ModuleScanner.scan], which produces a [TrustReport].
 *
 * Contract:
 * ```
 * ViewModel.previewUri(uri)
 *   → ModuleRepository.preview(uri)
 *     → ModuleInspector.inspect(context, uri)
 *       → ContentResolver.openInputStream(uri) × 2
 *       → ModuleScanner.scan(hashStream, manifestStream)
 *         → HashCalculator.sha256(...)
 *         → AvmManifestParser.parse(...)
 *         → RiskAnalyzer.analyze(...)
 *       → TrustReport
 * ```
 *
 * The `:core` scanner is pure JVM; this class is the only Android-aware
 * boundary. Replacing the source (e.g. with a `File` for CLI) only
 * requires re-implementing `inspect`.
 */
object ModuleInspector {

    /**
     * Scan a `.avm` package referenced by a content [uri] WITHOUT
     * installing it. Computes SHA-256, parses `module.json`, and
     * produces a [TrustReport].
     */
    suspend fun inspect(context: Context, uri: Uri): ScanResult {
        return try {
            val hashStream = context.contentResolver.openInputStream(uri)
            val manifestStream = context.contentResolver.openInputStream(uri)
            if (hashStream == null || manifestStream == null) {
                return ScanResult.Failure("Cannot open file at $uri")
            }
            val report = ModuleScanner.scan(hashStream, manifestStream)
            ScanResult.Success(report)
        } catch (e: Exception) {
            ScanResult.Failure(e.message ?: "Scan failed")
        }
    }
}

/**
 * Outcome of [ModuleInspector.inspect].
 *
 * Repository-level abstraction — the UI never sees the parser's own
 * `PreviewResult` type, so swapping the parser implementation (e.g.
 * AVM v4, remote manifest, signed manifest) does not ripple into
 * Compose.
 */
sealed class ScanResult {
    data class Success(val report: TrustReport) : ScanResult()
    data class Failure(val reason: String) : ScanResult()
}
