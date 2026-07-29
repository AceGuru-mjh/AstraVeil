package com.astraveil.core.modules.security

import com.astraveil.core.modules.model.ModuleManifestPreview

/**
 * Outcome of scanning a `.avm` package through the Module Trust Pipeline.
 *
 * Produced by [ModuleScanner.scan]. Aggregates:
 *  - **Package integrity** — SHA-256 fingerprint of the raw archive bytes.
 *  - **Manifest status** — was `module.json` present and parseable?
 *  - **Permission risk** — count, highest declared risk, source.
 *  - **Signature status** — has the package been signed / verified?
 *
 * The UI renders this as the "Security Review" step BEFORE the user
 * confirms install. This is the core differentiator between AstraVeil
 * and a naive root-module manager: the user sees the package
 * fingerprint and a real risk assessment, not a blind "Install" button.
 *
 * Pipeline (PR18.3):
 * ```
 * .avm URI
 *   → HashCalculator.sha256(stream)         [packageHash]
 *   → AvmManifestParser.parse(stream)        [ModuleManifestPreview]
 *   → RiskAnalyzer.analyze(preview)          [RiskAssessment]
 *   → TrustReport(...)
 * ```
 *
 * This type is pure data — no Android / Compose dependency — so the
 * daemon and CLI can reuse the same trust report shape.
 */
data class TrustReport(
    /** SHA-256 fingerprint of the raw `.avm` archive bytes, lowercase hex. */
    val packageHash: String,

    /** Whether `module.json` was found and parsed. */
    val manifestStatus: ManifestStatus,

    /**
     * The parsed manifest preview, or `null` when [manifestStatus] is
     * not [ManifestStatus.OK]. The UI shows the identity / permissions
     * block only when this is non-null.
     */
    val preview: ModuleManifestPreview?,

    /** Number of declared permissions (0 when manifest failed). */
    val permissionCount: Int,

    /**
     * Highest declared risk across all permissions, or `null` when no
     * permission declared a risk level (Phase-0 manifest) or the
     * manifest failed to parse.
     */
    val highestRisk: Int?,

    /** Where the risk data came from. */
    val riskSource: RiskSource,

    /** Signature verification state. */
    val signatureStatus: SignatureStatus,

    /** Overall risk level, derived from [highestRisk] by [RiskAnalyzer]. */
    val overallRiskLevel: RiskLevel,
) {
    /** Convenience: did every check pass well enough to offer "Install"? */
    val isInstallable: Boolean
        get() = manifestStatus == ManifestStatus.OK && signatureStatus != SignatureStatus.REJECTED
}

/** State of the `module.json` extraction + parse step. */
enum class ManifestStatus {
    /** `module.json` found and parsed successfully. */
    OK,

    /** `module.json` not present in the archive. */
    MISSING,

    /** `module.json` present but could not be parsed. */
    MALFORMED,
}

/**
 * Provenance of the risk numbers in the report.
 *
 *  - [MANIFEST] — at least one permission declared a `riskLevel` (v3).
 *  - [UNDECLARED] — the manifest used the Phase-0 string-only format;
 *    no risk was declared, so [TrustReport.highestRisk] is `null` and
 *    the UI shows "Unknown".
 *  - [NONE] — no permissions declared at all, or the manifest failed.
 */
enum class RiskSource {
    MANIFEST,
    UNDECLARED,
    NONE,
}

/**
 * Signature verification state.
 *
 * PR18.3 ships [UNKNOWN] — real signature verification (ed25519 /
 * minisign) is a later milestone. The UI surfaces this honestly so
 * the user knows the package has NOT been authenticated.
 */
enum class SignatureStatus {
    /** Signature not checked yet (default for PR18.3). */
    UNKNOWN,

    /** Package is unsigned. */
    UNSIGNED,

    /** Signature present but could not be verified against a trusted key. */
    UNVERIFIED,

    /** Signature verified against a trusted author key. */
    VERIFIED,

    /** Signature present but invalid (tampered / wrong key). */
    REJECTED,
}

/**
 * Overall risk level, derived from [TrustReport.highestRisk].
 *
 * Thresholds aligned with the Rust policy engine:
 *  - null          → UNKNOWN
 *  - 0..30         → LOW
 *  - 31..70        → MEDIUM
 *  - 71..89        → HIGH
 *  - 90+           → CRITICAL
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    UNKNOWN,
}
