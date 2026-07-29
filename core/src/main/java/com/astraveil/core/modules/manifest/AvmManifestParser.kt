package com.astraveil.core.modules.manifest

import com.astraveil.core.modules.model.ModuleInfo
import com.astraveil.core.modules.model.ModulePermissionInfo
import com.astraveil.core.modules.model.ModuleUiState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Pre-parses an `.avm` package to extract its manifest WITHOUT installing.
 *
 * `.avm` format: standard ZIP containing at minimum:
 * ```
 * module.json        — manifest (required)
 * lib/module.so      — native binary (ignored during preview)
 * ```
 *
 * Supports two manifest formats:
 * - **Phase 0**: `{ "name", "version", "api", "permissions": ["str"] }`
 *   → permissions carry NO risk data; [ModulePermissionInfo.risk] is `null`
 *     (rendered as "Unknown" by the UI).
 * - **v3**:      `{ "id", "name", "version", "apiVersion",
 *                  "permissions": [{capability, reason, riskLevel}] }`
 *   → risk comes straight from the manifest's `riskLevel` field.
 *
 * Patch 18.2.1: the parser no longer runs heuristics to fabricate a risk
 * score for Phase-0 manifests. Undeclared risk = `null` = "Unknown".
 *
 * This class is stateless and thread-safe.
 */
object AvmManifestParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Result of a preview parse.
     */
    sealed class PreviewResult {
        data class Success(val module: ModuleInfo) : PreviewResult()
        data class Failure(val reason: PreviewError) : PreviewResult()
    }

    enum class PreviewError(val message: String) {
        NOT_A_ZIP("File is not a valid .avm archive (expected ZIP)."),
        MISSING_MANIFEST("module.json not found inside the .avm archive."),
        MALFORMED_MANIFEST("module.json exists but could not be parsed."),
        EMPTY_PERMISSIONS("Manifest parsed but contains no permission declarations."),
        IO_ERROR("I/O error while reading the archive."),
    }

    /**
     * Parse an `.avm` from a raw [InputStream].
     *
     * The stream is read sequentially via [ZipInputStream]; only the
     * `module.json` entry is extracted. The rest of the archive
     * (including potentially large `.so` binaries) is skipped.
     *
     * @return [PreviewResult.Success] with a fully populated [ModuleInfo],
     *         or [PreviewResult.Failure] with a specific reason.
     */
    fun parse(inputStream: InputStream): PreviewResult {
        val manifestJson: String?
        try {
            manifestJson = extractManifestJson(inputStream)
        } catch (e: Exception) {
            return PreviewResult.Failure(PreviewError.IO_ERROR)
        }

        if (manifestJson == null) {
            return PreviewResult.Failure(PreviewError.MISSING_MANIFEST)
        }

        return parseManifestJson(manifestJson)
    }

    // ---- internal ----

    /**
     * Walk the ZIP entries and return the content of the first
     * entry whose name is `module.json` or ends with `/module.json`.
     * Returns null if no such entry exists.
     */
    private fun extractManifestJson(inputStream: InputStream): String? {
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        while (entry != null) {
            val name = entry.name
            if (name == "module.json" || name.endsWith("/module.json")) {
                val bytes = zip.readBytes()
                zip.closeEntry()
                return bytes.decodeToString()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        return null
    }

    /**
     * Detect manifest format and parse accordingly.
     *
     * Detection heuristic:
     * - v3 manifests contain `"id"` AND `"apiVersion"` keys.
     * - Phase 0 manifests contain `"name"` AND `"api"` keys.
     * - If neither matches, attempt v3 first, then Phase 0.
     */
    private fun parseManifestJson(raw: String): PreviewResult {
        // Try v3 first (richer data: riskLevel + reason per permission)
        try {
            val v3 = json.decodeFromString<V3Manifest>(raw)
            if (v3.id.isNotBlank()) {
                return PreviewResult.Success(v3.toModuleInfo())
            }
        } catch (_: Exception) {
            // fall through to Phase 0
        }

        // Try Phase 0
        try {
            val p0 = json.decodeFromString<Phase0Manifest>(raw)
            if (p0.name.isNotBlank()) {
                return PreviewResult.Success(p0.toModuleInfo())
            }
        } catch (_: Exception) {
            // fall through to error
        }

        return PreviewResult.Failure(PreviewError.MALFORMED_MANIFEST)
    }

    // ---- v3 format ----

    @Serializable
    private data class V3Manifest(
        val id: String = "",
        val name: String = "",
        val version: String = "0.0.0",
        val apiVersion: Int = 2,
        val description: String = "",
        val permissions: List<V3Permission> = emptyList(),
    )

    @Serializable
    private data class V3Permission(
        val capability: String = "",
        val reason: String = "",
        val riskLevel: Int = 0,
    )

    private fun V3Manifest.toModuleInfo(): ModuleInfo = ModuleInfo(
        id = id,
        name = name.ifBlank { id },
        version = version,
        description = description,
        state = ModuleUiState.INSTALLED, // preview: not yet installed
        permissions = permissions.map { p ->
            ModulePermissionInfo(
                capability = p.capability,
                risk = p.riskLevel,           // ← declared by manifest
                reason = p.reason,
            )
        },
    )

    // ---- Phase 0 format ----
    //
    // Phase-0 manifests carry ONLY a flat list of permission string tokens.
    // There is NO risk information. Per Patch 18.2.1 we surface this
    // honestly as `risk = null` rather than inventing a heuristic score.

    @Serializable
    private data class Phase0Manifest(
        val name: String = "",
        val version: String = "0.0.0",
        val api: Int = 1,
        val description: String = "",
        val permissions: List<String> = emptyList(),
    )

    private fun Phase0Manifest.toModuleInfo(): ModuleInfo = ModuleInfo(
        id = name,
        name = name,
        version = version,
        description = description,
        state = ModuleUiState.INSTALLED,
        permissions = permissions.map { perm ->
            ModulePermissionInfo(
                capability = perm,
                risk = null,                 // ← undeclared → UI shows "Unknown"
                reason = "",
            )
        },
    )
}
