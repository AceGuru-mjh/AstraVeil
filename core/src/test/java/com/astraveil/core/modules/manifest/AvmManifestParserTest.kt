package com.astraveil.core.modules.manifest

import com.astraveil.core.modules.manifest.AvmManifestParser.PreviewError
import com.astraveil.core.modules.manifest.AvmManifestParser.PreviewResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Unit tests for [AvmManifestParser] — the ZIP+JSON manifest extractor
 * at the core of the Module Trust Pipeline (PR18.3).
 *
 * Builds in-memory `.avm` archives so the tests run on plain JVM with
 * no filesystem dependency.
 */
class AvmManifestParserTest {

    private val parser = AvmManifestParser

    /** Build a ZIP byte array containing the given entry name → content. */
    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
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
    fun `parses v3 manifest with id and declared risks`() {
        val manifest = """
            {
              "id": "com.example.mod",
              "name": "Example Module",
              "version": "1.2.0",
              "apiVersion": 3,
              "description": "A test module",
              "permissions": [
                {"capability": "root_execution", "reason": "su", "riskLevel": 90},
                {"capability": "filesystem", "reason": "logs", "riskLevel": 30}
              ]
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals("com.example.mod", preview.id)
        assertEquals("Example Module", preview.name)
        assertEquals("1.2.0", preview.version)
        assertEquals(2, preview.permissions.size)
        assertEquals("root_execution", preview.permissions[0].capability)
        assertEquals(90, preview.permissions[0].risk)
        assertEquals("su", preview.permissions[0].reason)
        assertEquals(30, preview.permissions[1].risk)
    }

    @Test
    fun `parses Phase-0 manifest with string permissions and null risks`() {
        val manifest = """
            {
              "name": "LegacyModule",
              "version": "0.9.0",
              "api": 1,
              "permissions": ["mount", "network", "filesystem"]
            }
        """.trimIndent()
        val avm = zip("module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals("LegacyModule", preview.id)
        assertEquals(3, preview.permissions.size)
        // Phase-0: risk MUST be null, never fabricated.
        preview.permissions.forEach { assertNull(it.risk) }
        assertEquals("mount", preview.permissions[0].capability)
    }

    @Test
    fun `missing module.json returns MISSING_MANIFEST`() {
        val avm = zip("README.txt" to "not a manifest")
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Failure)
        assertEquals(
            PreviewError.MISSING_MANIFEST,
            (result as PreviewResult.Failure).reason,
        )
    }

    @Test
    fun `malformed JSON returns MALFORMED_MANIFEST`() {
        val avm = zip("module.json" to "{ this is not valid json }}}")
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Failure)
        assertEquals(
            PreviewError.MALFORMED_MANIFEST,
            (result as PreviewResult.Failure).reason,
        )
    }

    @Test
    fun `manifest nested in subdirectory is still found`() {
        val manifest = """{"name":"Nested","version":"1.0.0","api":1,"permissions":[]}"""
        val avm = zip("subdir/module.json" to manifest)
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Success)
        val preview = (result as PreviewResult.Success).preview
        assertEquals("Nested", preview.name)
    }

    @Test
    fun `empty archive returns MISSING_MANIFEST`() {
        val avm = zip() // no entries at all
        val result = parser.parse(ByteArrayInputStream(avm))

        assertTrue(result is PreviewResult.Failure)
        assertEquals(
            PreviewError.MISSING_MANIFEST,
            (result as PreviewResult.Failure).reason,
        )
    }
}
