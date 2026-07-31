package com.astraveil.modules

import com.astraveil.sdk.AstraSdkConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local [fail] helper that returns [Nothing], so it can substitute for any type
 * in `Result.getOrElse { fail(...) }` without losing the manifest's type info
 * (org.junit.Assert.fail returns Unit which would widen the inferred type).
 */
private fun fail(message: String): Nothing = throw AssertionError(message)

/**
 * Unit tests for [ModuleValidator.validateManifest] — the String overload
 * that parses a `module.json` payload without touching the filesystem.
 *
 * Pinpoints each branch of the install-time validation contract:
 *  * required fields (name, version)
 *  * api / minApi bounds against [AstraSdkConstants.MODULE_API_LEVEL]
 *  * permission whitelist against [AstraSdkConstants.SUPPORTED_PERMISSIONS]
 *  * malformed / empty JSON
 *
 * All assertions go through `Result` accessors so failure cases surface the
 * underlying exception via `getOrElse { fail(...) }` for diagnostics.
 */
class ModuleValidatorTest {

    private val validator = ModuleValidator()

    @Test
    fun valid_manifest_passes() {
        val raw = """
            {
              "name": "com.example.coolmod",
              "version": "1.0.0",
              "api": 1,
              "permissions": ["filesystem"]
            }
        """.trimIndent()

        val result = validator.validateManifest(raw)

        assertTrue(result.isSuccess)
        val manifest = result.getOrElse { fail("expected success, got: ${it.message}") }
        assertEquals("com.example.coolmod", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals(1, manifest.api)
        assertEquals(listOf("filesystem"), manifest.permissions)
    }

    @Test
    fun missing_name_fails() {
        val raw = """{"version":"1.0.0"}"""

        val result = validator.validateManifest(raw)

        assertFalse("missing 'name' must fail", result.isSuccess)
    }

    @Test
    fun missing_version_fails() {
        val raw = """{"name":"mod"}"""

        val result = validator.validateManifest(raw)

        assertFalse("missing 'version' must fail", result.isSuccess)
    }

    @Test
    fun api_zero_fails() {
        val raw = """
            {
              "name": "com.example.mod",
              "version": "1.0.0",
              "api": 0
            }
        """.trimIndent()

        val result = validator.validateManifest(raw)

        assertFalse("api=0 must fail (must be positive)", result.isSuccess)
    }

    @Test
    fun api_exceeds_level_fails() {
        // AstraSdkConstants.MODULE_API_LEVEL == 1; api=2 must be rejected.
        assertEquals(1, AstraSdkConstants.MODULE_API_LEVEL)
        val raw = """
            {
              "name": "com.example.mod",
              "version": "1.0.0",
              "api": 2
            }
        """.trimIndent()

        val result = validator.validateManifest(raw)

        assertFalse("api > MODULE_API_LEVEL must fail", result.isSuccess)
    }

    @Test
    fun unsupported_permission_fails() {
        val raw = """
            {
              "name": "com.example.mod",
              "version": "1.0.0",
              "api": 1,
              "permissions": ["su"]
            }
        """.trimIndent()

        val result = validator.validateManifest(raw)

        assertFalse("unsupported permission token 'su' must fail", result.isSuccess)
    }

    @Test
    fun supported_permissions_pass() {
        // Every token in AstraSdkConstants.SUPPORTED_PERMISSIONS in one manifest.
        val raw = """
            {
              "name": "com.example.mod",
              "version": "1.0.0",
              "api": 1,
              "permissions": ["filesystem","network","shell","mount","namespace","property"]
            }
        """.trimIndent()

        val result = validator.validateManifest(raw)

        assertTrue(result.isSuccess)
        val manifest = result.getOrElse { fail("expected success, got: ${it.message}") }
        assertEquals(
            listOf("filesystem", "network", "shell", "mount", "namespace", "property"),
            manifest.permissions
        )
    }

    @Test
    fun empty_permissions_pass() {
        val raw = """
            {
              "name": "com.example.mod",
              "version": "1.0.0",
              "api": 1,
              "permissions": []
            }
        """.trimIndent()

        val result = validator.validateManifest(raw)

        assertTrue(result.isSuccess)
        val manifest = result.getOrElse { fail("expected success, got: ${it.message}") }
        assertTrue(manifest.permissions.isEmpty())
    }

    @Test
    fun valid_manifest_with_all_fields() {
        val raw = """
            {
              "name": "com.example.fullmod",
              "version": "2.3.1",
              "api": 1,
              "author": "AstraVeil",
              "description": "A full-featured test module",
              "permissions": ["filesystem","mount"],
              "runtime": "runtime/arm64.so",
              "entry": "astra_module_entry",
              "minApi": 1
            }
        """.trimIndent()

        val result = validator.validateManifest(raw)

        assertTrue(result.isSuccess)
        val manifest = result.getOrElse { fail("expected success, got: ${it.message}") }
        assertEquals("com.example.fullmod", manifest.name)
        assertEquals("2.3.1", manifest.version)
        assertEquals(1, manifest.api)
        assertEquals("AstraVeil", manifest.author)
        assertEquals("A full-featured test module", manifest.description)
        assertEquals(listOf("filesystem", "mount"), manifest.permissions)
        assertEquals("runtime/arm64.so", manifest.runtime)
        assertEquals("astra_module_entry", manifest.entry)
        assertEquals(1, manifest.minApi)
    }

    @Test
    fun minApi_exceeds_level_fails() {
        // AstraSdkConstants.MODULE_API_LEVEL == 1; minApi=2 must be rejected.
        val raw = """
            {
              "name": "com.example.mod",
              "version": "1.0.0",
              "api": 1,
              "minApi": 2
            }
        """.trimIndent()

        val result = validator.validateManifest(raw)

        assertFalse("minApi > MODULE_API_LEVEL must fail", result.isSuccess)
    }

    @Test
    fun malformed_json_fails() {
        val raw = "{bad"

        val result = validator.validateManifest(raw)

        assertFalse("malformed JSON must fail parsing", result.isSuccess)
    }

    @Test
    fun empty_json_fails() {
        // An empty JSON object parses but has blank name/version — both must
        // be rejected by the require(...) checks in validateManifest.
        val raw = "{}"

        val result = validator.validateManifest(raw)

        assertFalse("empty JSON object must fail (blank name)", result.isSuccess)
    }
}
