package com.astraveil.core.modules.security

import com.astraveil.core.modules.model.ModuleManifestPreview
import com.astraveil.core.modules.model.PermissionDeclaration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [RiskAnalyzer] — the risk aggregation step of the
 * Module Trust Pipeline (PR18.3).
 *
 * These tests pin the core trust guarantees:
 *  - No heuristic fabrication: a Phase-0 manifest (all-null risks) MUST
 *    produce UNKNOWN, never an invented score.
 *  - v3 manifests: the max declared riskLevel drives the level.
 *  - Thresholds: 0-30 LOW, 31-70 MEDIUM, 71-89 HIGH, 90+ CRITICAL.
 */
class RiskAnalyzerTest {

    private val analyzer = RiskAnalyzer

    @Test
    fun `null preview produces NONE risk source and UNKNOWN level`() {
        val assessment = analyzer.analyze(null)
        assertEquals(0, assessment.permissionCount)
        assertNull(assessment.highestRisk)
        assertEquals(RiskSource.NONE, assessment.riskSource)
        assertEquals(RiskLevel.UNKNOWN, assessment.overallRiskLevel)
    }

    @Test
    fun `empty permissions produces NONE risk source`() {
        val preview = ModuleManifestPreview(
            id = "com.test.empty",
            name = "Empty",
            version = "1.0.0",
            description = "",
            permissions = emptyList(),
        )
        val assessment = analyzer.analyze(preview)
        assertEquals(0, assessment.permissionCount)
        assertEquals(RiskSource.NONE, assessment.riskSource)
        assertEquals(RiskLevel.UNKNOWN, assessment.overallRiskLevel)
    }

    @Test
    fun `Phase-0 manifest with string-only permissions produces UNDECLARED and UNKNOWN`() {
        val preview = ModuleManifestPreview(
            id = "com.test.phase0",
            name = "Phase0Module",
            version = "1.0.0",
            description = "",
            permissions = listOf(
                PermissionDeclaration(capability = "root_execution", risk = null),
                PermissionDeclaration(capability = "filesystem", risk = null),
            ),
        )
        val assessment = analyzer.analyze(preview)
        assertEquals(2, assessment.permissionCount)
        assertNull(assessment.highestRisk)
        assertEquals(RiskSource.UNDECLARED, assessment.riskSource)
        assertEquals(RiskLevel.UNKNOWN, assessment.overallRiskLevel)
    }

    @Test
    fun `v3 manifest takes max declared riskLevel`() {
        val preview = ModuleManifestPreview(
            id = "com.test.v3",
            name = "V3Module",
            version = "2.0.0",
            description = "",
            permissions = listOf(
                PermissionDeclaration(capability = "filesystem", risk = 20, reason = "logs"),
                PermissionDeclaration(capability = "root_execution", risk = 90, reason = "su"),
                PermissionDeclaration(capability = "network", risk = 40, reason = "telemetry"),
            ),
        )
        val assessment = analyzer.analyze(preview)
        assertEquals(3, assessment.permissionCount)
        assertEquals(90, assessment.highestRisk)
        assertEquals(RiskSource.MANIFEST, assessment.riskSource)
        assertEquals(RiskLevel.CRITICAL, assessment.overallRiskLevel)
    }

    @Test
    fun `mixed v3 and Phase-0 permissions use MANIFEST source with max declared`() {
        val preview = ModuleManifestPreview(
            id = "com.test.mixed",
            name = "Mixed",
            version = "1.0.0",
            description = "",
            permissions = listOf(
                PermissionDeclaration(capability = "filesystem", risk = null),
                PermissionDeclaration(capability = "mount", risk = 70, reason = "overlays"),
            ),
        )
        val assessment = analyzer.analyze(preview)
        assertEquals(2, assessment.permissionCount)
        assertEquals(70, assessment.highestRisk)
        assertEquals(RiskSource.MANIFEST, assessment.riskSource)
        assertEquals(RiskLevel.MEDIUM, assessment.overallRiskLevel)
    }

    @Test
    fun `levelFor thresholds`() {
        assertEquals(RiskLevel.UNKNOWN, analyzer.levelFor(null))
        assertEquals(RiskLevel.LOW, analyzer.levelFor(0))
        assertEquals(RiskLevel.LOW, analyzer.levelFor(30))
        assertEquals(RiskLevel.MEDIUM, analyzer.levelFor(31))
        assertEquals(RiskLevel.MEDIUM, analyzer.levelFor(70))
        assertEquals(RiskLevel.HIGH, analyzer.levelFor(71))
        assertEquals(RiskLevel.HIGH, analyzer.levelFor(89))
        assertEquals(RiskLevel.CRITICAL, analyzer.levelFor(90))
        assertEquals(RiskLevel.CRITICAL, analyzer.levelFor(100))
    }
}
