package com.astraveil.core.modules.manifest

import com.astraveil.core.modules.manifest.AvmManifestParser.PreviewError
import com.astraveil.core.modules.manifest.AvmManifestParser.PreviewResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Edge-case unit tests for [AvmManifestParser].
 *
 * This file is ADDITIVE: it complements the existing `AvmManifestParserTest`
 * (which covers the happy-path v3/Phase-0 parse, missing manifest, malformed
 * JSON, nested manifest, and empty archive). The 9 cases below probe the
 * remaining edges of the parser's contract:
 *
 *   - Empty `permissions` arrays for both formats.
 *   - Phase-0 risk handling post PR #37 / Patch 18.2.1: every Phase-0
 *     permission carries `risk = null` regardless of the capability name
 *     (no heuristics are fabricated). This is verified for `su`,
 *     `kernel_hook`, and an unknown capability token.
 *   - v3 manifests tolerate unknown JSON keys (forward-compat).
 *   - Non-ZIP input yields a `Failure` result, never a thrown exception.
 *   - A v3 manifest whose `id` is blank falls through to the Phase-0 path.
 *   - v3 manifests with multiple permissions preserve ordering and risks.
 *
 * All tests build in-memory `.avm` ZIP archives so they run on plain JVM
 * with no filesystem dependency.
 */
class AvmManifestParserEdgeCasesTest {

    private val parser = AvmManifestParser

    /** Build a ZIP byte array containing the given entry name → content. */
    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    @Test
    fun v3_with_empty_permissions() {
        // A v3 manifest with an explicit empty permissions array is a
        // valid (if boring) module: the parser returns Success with a
        // preview whose permissions list is empty.
        val manifest = """
            {
              "id": "com.example.empty",
              "name": "Empty",
              "version": "0.1.0",
              "apiVersion": 3,
              "permissions": []
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals("com.example.empty", preview.id)
        assertTrue(preview.permissions.isEmpty())
    }

    @Test
    fun phase0_with_empty_permissions() {
        // Same idea, Phase-0 side: empty permissions array is valid.
        val manifest = """
            {
              "name": "EmptyLegacy",
              "version": "0.1.0",
              "api": 1,
              "permissions": []
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals("EmptyLegacy", preview.id)
        assertTrue(preview.permissions.isEmpty())
    }

    @Test
    fun phase0_risk_estimation_su() {
        // PR #37 / Patch 18.2.1: Phase-0 manifests carry NO risk data, and
        // the parser fabricates none. Even a capability as dangerous as
        // "su" surfaces with `risk == null` (rendered as "Unknown" by the
        // UI). The old heuristic that mapped su → 90 is GONE.
        val manifest = """
            {
              "name": "SuModule",
              "version": "0.1.0",
              "api": 1,
              "permissions": ["su"]
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals(1, preview.permissions.size)
        assertEquals("su", preview.permissions[0].capability)
        assertNull(preview.permissions[0].risk)
    }

    @Test
    fun phase0_risk_estimation_kernel_hook() {
        // Same contract as `phase0_risk_estimation_su` for the
        // `kernel_hook` capability: risk is null, not a fabricated 100.
        val manifest = """
            {
              "name": "HookModule",
              "version": "0.1.0",
              "api": 1,
              "permissions": ["kernel_hook"]
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals("kernel_hook", preview.permissions[0].capability)
        assertNull(preview.permissions[0].risk)
    }

    @Test
    fun phase0_unknown_capability() {
        // An unrecognized capability token on a Phase-0 manifest is still
        // surfaced — with `risk == null`. The parser does NOT reject
        // unknown capabilities; trust evaluation happens downstream in
        // the security module.
        val manifest = """
            {
              "name": "MysteryModule",
              "version": "0.1.0",
              "api": 1,
              "permissions": ["quantum_entangle"]
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals("quantum_entangle", preview.permissions[0].capability)
        assertNull(preview.permissions[0].risk)
    }

    @Test
    fun v3_unknown_keys_ignored() {
        // The parser's Json instance is configured with
        // `ignoreUnknownKeys = true`. A v3 manifest carrying extra
        // vendor-specific keys must still parse successfully.
        val manifest = """
            {
              "id": "com.example.unknown",
              "name": "UnknownKeys",
              "version": "1.0.0",
              "apiVersion": 3,
              "permissions": [],
              "vendor": "acme",
              "minSdk": 30,
              "experimental": { "featureFlags": ["a", "b"] }
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals("com.example.unknown", preview.id)
        assertEquals("UnknownKeys", preview.name)
        assertEquals("1.0.0", preview.version)
        assertTrue(preview.permissions.isEmpty())
    }

    @Test
    fun not_a_zip_returns_failure() {
        // Plain text input is not a valid ZIP archive. The parser MUST NOT
        // throw — it returns a `Failure` result. On the reference JVM the
        // `ZipInputStream` implementation treats the malformed local-file
        // header as "no entries found" (rather than raising), so the
        // parser surfaces `MISSING_MANIFEST`; the only hard contract this
        // test pins is that some `Failure` is returned.
        val plainText = "this is not a zip file".toByteArray()
        val result = parser.parse(ByteArrayInputStream(plainText))

        assertTrue(result is PreviewResult.Failure)
        val reason = (result as PreviewResult.Failure).reason
        // Either reason is acceptable on different JVMs; both signal
        // "we could not extract a manifest from this input".
        assertTrue(
            "expected MISSING_MANIFEST or IO_ERROR but was $reason",
            reason == PreviewError.MISSING_MANIFEST ||
                reason == PreviewError.IO_ERROR,
        )
    }

    @Test
    fun v3_with_blank_id_falls_through_to_phase0() {
        // A v3 manifest whose `id` is blank must NOT be returned as v3
        // (the v3 path requires `id.isNotBlank()`). With empty
        // permissions and a non-blank `name`, the same JSON should be
        // re-parsed as Phase-0 — where `id` is derived from `name`.
        val manifest = """
            {
              "id": "",
              "name": "PhaseZeroFallback",
              "version": "1.0.0",
              "apiVersion": 3,
              "permissions": []
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        // Phase-0 derives id from name; both should equal "PhaseZeroFallback".
        assertEquals("PhaseZeroFallback", preview.id)
        assertEquals("PhaseZeroFallback", preview.name)
        assertTrue(preview.permissions.isEmpty())
    }

    @Test
    fun v3_multiple_permissions() {
        // Three permissions with distinct capability/reason/risk values
        // must round-trip in declared order through the v3 path.
        val manifest = """
            {
              "id": "com.example.multi",
              "name": "MultiPerm",
              "version": "2.0.0",
              "apiVersion": 3,
              "permissions": [
                {"capability": "filesystem", "reason": "logs", "riskLevel": 20},
                {"capability": "network", "reason": "telemetry", "riskLevel": 40},
                {"capability": "su", "reason": "root", "riskLevel": 90}
              ]
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals(3, preview.permissions.size)

        assertEquals("filesystem", preview.permissions[0].capability)
        assertEquals(20, preview.permissions[0].risk)
        assertEquals("logs", preview.permissions[0].reason)

        assertEquals("network", preview.permissions[1].capability)
        assertEquals(40, preview.permissions[1].risk)
        assertEquals("telemetry", preview.permissions[1].reason)

        assertEquals("su", preview.permissions[2].capability)
        assertEquals(90, preview.permissions[2].risk)
        assertEquals("root", preview.permissions[2].reason)
    }
}
