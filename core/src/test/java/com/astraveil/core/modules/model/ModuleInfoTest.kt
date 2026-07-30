package com.astraveil.core.modules.model

import com.astraveil.core.modules.security.ManifestStatus
import com.astraveil.core.modules.security.RiskLevel
import com.astraveil.core.modules.security.RiskSource
import com.astraveil.core.modules.security.SignatureStatus
import com.astraveil.core.modules.security.TrustReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the UI-facing module data model:
 *  - [ModuleInfo] data-class equality / copy semantics;
 *  - [ModulePermissionInfo] default `reason` and nullable `risk`;
 *  - [ModuleUiState] enum completeness;
 *  - [TrustReport.isInstallable] computed property under various
 *    manifest + signature combinations;
 *  - [RiskLevel] / [ManifestStatus] enum completeness.
 *
 * Pure JVM — no Android, no Compose.
 */
class ModuleInfoTest {

    @Test
    fun `moduleInfo_equality`() {
        val perm = ModulePermissionInfo(capability = "filesystem", risk = 10, reason = "rw")
        val a = ModuleInfo(
            id = "mod.a",
            name = "Module A",
            version = "1.0.0",
            description = "desc",
            state = ModuleUiState.INSTALLED,
            permissions = listOf(perm),
        )
        val b = ModuleInfo(
            id = "mod.a",
            name = "Module A",
            version = "1.0.0",
            description = "desc",
            state = ModuleUiState.INSTALLED,
            permissions = listOf(perm),
        )
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `moduleInfo_copy`() {
        val orig = ModuleInfo(
            id = "mod.a",
            name = "Module A",
            version = "1.0.0",
            description = "desc",
            state = ModuleUiState.INSTALLED,
            permissions = emptyList(),
        )
        val copy = orig.copy(state = ModuleUiState.RUNNING)
        assertEquals(ModuleUiState.RUNNING, copy.state)
        // Other fields preserved.
        assertEquals(orig.id, copy.id)
        assertEquals(orig.name, copy.name)
        assertEquals(orig.version, copy.version)
    }

    @Test
    fun `modulePermissionInfo_default_reason_empty`() {
        val p = ModulePermissionInfo(capability = "filesystem", risk = 5)
        assertEquals("", p.reason)
    }

    @Test
    fun `modulePermissionInfo_with_null_risk`() {
        val p = ModulePermissionInfo(capability = "filesystem", risk = null)
        assertNull(p.risk)
        // Default reason still applies.
        assertEquals("", p.reason)
    }

    @Test
    fun `moduleUiState_enum_has_4_values`() {
        val values = ModuleUiState.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(ModuleUiState.INSTALLED))
        assertTrue(values.contains(ModuleUiState.RUNNING))
        assertTrue(values.contains(ModuleUiState.STOPPED))
        assertTrue(values.contains(ModuleUiState.FAILED))
    }

    @Test
    fun `trustReport_isInstallable_true_when_manifest_ok_and_sig_not_rejected`() {
        val report = TrustReport(
            packageHash = "abc",
            manifestStatus = ManifestStatus.OK,
            preview = null,
            permissionCount = 0,
            highestRisk = null,
            riskSource = RiskSource.NONE,
            signatureStatus = SignatureStatus.UNKNOWN,
            overallRiskLevel = RiskLevel.UNKNOWN,
        )
        assertTrue(report.isInstallable)
    }

    @Test
    fun `trustReport_isInstallable_false_when_manifest_missing`() {
        val report = TrustReport(
            packageHash = "abc",
            manifestStatus = ManifestStatus.MISSING,
            preview = null,
            permissionCount = 0,
            highestRisk = null,
            riskSource = RiskSource.NONE,
            signatureStatus = SignatureStatus.VERIFIED,
            overallRiskLevel = RiskLevel.LOW,
        )
        assertFalse(report.isInstallable)
    }

    @Test
    fun `trustReport_isInstallable_false_when_signature_rejected`() {
        val report = TrustReport(
            packageHash = "abc",
            manifestStatus = ManifestStatus.OK,
            preview = null,
            permissionCount = 0,
            highestRisk = null,
            riskSource = RiskSource.NONE,
            signatureStatus = SignatureStatus.REJECTED,
            overallRiskLevel = RiskLevel.UNKNOWN,
        )
        assertFalse(report.isInstallable)
    }

    @Test
    fun `riskLevel_enum_has_5_values`() {
        val values = RiskLevel.values()
        assertEquals(5, values.size)
        assertTrue(values.contains(RiskLevel.LOW))
        assertTrue(values.contains(RiskLevel.MEDIUM))
        assertTrue(values.contains(RiskLevel.HIGH))
        assertTrue(values.contains(RiskLevel.CRITICAL))
        assertTrue(values.contains(RiskLevel.UNKNOWN))
    }

    @Test
    fun `manifestStatus_enum_has_3_values`() {
        val values = ManifestStatus.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(ManifestStatus.OK))
        assertTrue(values.contains(ManifestStatus.MISSING))
        assertTrue(values.contains(ManifestStatus.MALFORMED))
    }
}
