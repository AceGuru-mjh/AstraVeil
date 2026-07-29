package com.astraveil.app.repository

import android.content.Context
import android.net.Uri
import com.astraveil.core.modules.security.ModuleScanner
import com.astraveil.core.modules.security.TrustReport
import java.io.ByteArrayInputStream

/**
 * High-level `:app` facade over the `:core` Module Trust Pipeline (PR18.3).
 *
 * Owns the Android `ContentResolver` interaction: reads the picked `.avm`
 * URI into a `ByteArray` ONCE, then feeds two independent
 * `ByteArrayInputStream`s into [ModuleScanner.scan] (one for SHA-256,
 * one for manifest parsing — a stream cannot be rewound).
 *
 * Why buffer to memory rather than calling `openInputStream(uri)` twice:
 *  - Some content providers hand out one-shot URIs whose stream cannot
 *    be reopened (the second `openInputStream` returns null or throws).
 *  - `.avm` packages are small (typically < 5 MB); the memory cost is
 *    negligible and the robustness gain is significant.
 *  - The install path (`ModuleRepository.install`) already buffers the
 *    whole file to a temp `File`, so this is consistent.
 *
 * Contract:
 * ```
 * ViewModel.previewUri(uri)
 *   → ModuleRepository.preview(uri)
 *     → ModuleInspector.inspect(context, uri)
 *       → ContentResolver.openInputStream(uri)   [once]
 *       → readBytes()                            [buffer]
 *       → ByteArrayInputStream × 2
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
            // Read the whole archive into memory once. See class KDoc
            // for why we don't reopen the stream for hash vs. manifest.
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ScanResult.Failure("Cannot open file at $uri")

            val hashStream = ByteArrayInputStream(bytes)
            val manifestStream = ByteArrayInputStream(bytes)
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
