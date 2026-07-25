package com.astraveil.modules

import com.astraveil.sdk.AstraSdkConstants
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Stateless validator for `.avm` packages.
 *
 * Used by [ModuleManager] at install time to reject malformed or unsupported
 * packages BEFORE they are unpacked or executed. All checks return
 * `Result<ModuleManifest>` so callers can chain them with `getOrElse` and
 * produce clean error messages in the UI.
 */
class ModuleValidator {

    /** Shared lenient JSON parser — unknown keys are ignored, missing
     *  optional fields fall back to defaults defined on [ModuleManifest]. */
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Parse and validate a `module.json` payload.
     *
     * Validation rules:
     *  1. The payload must deserialise into a [ModuleManifest].
     *  2. [ModuleManifest.name] must be non-blank.
     *  3. [ModuleManifest.version] must be non-blank.
     *  4. [ModuleManifest.api] must be `<=` [AstraSdkConstants.MODULE_API_LEVEL].
     *  5. Every entry in [ModuleManifest.permissions] must be in
     *     [AstraSdkConstants.SUPPORTED_PERMISSIONS].
     */
    fun validateManifest(raw: String): Result<ModuleManifest> = runCatching {
        val manifest = json.decodeFromString(ModuleManifest.serializer(), raw)

        require(manifest.name.isNotBlank()) { "module.json: 'name' must not be blank" }
        require(manifest.version.isNotBlank()) { "module.json: 'version' must not be blank" }
        require(manifest.api > 0) { "module.json: 'api' must be a positive integer" }
        require(manifest.api <= AstraSdkConstants.MODULE_API_LEVEL) {
            "module.json: 'api'=${manifest.api} exceeds SDK API level " +
                "${AstraSdkConstants.MODULE_API_LEVEL}; upgrade AstraVeil or pin an older module version"
        }
        require(manifest.minApi <= AstraSdkConstants.MODULE_API_LEVEL) {
            "module.json: 'minApi'=${manifest.minApi} exceeds current API level " +
                AstraSdkConstants.MODULE_API_LEVEL
        }
        val unknown = manifest.permissions - AstraSdkConstants.SUPPORTED_PERMISSIONS.toSet()
        require(unknown.isEmpty()) {
            "module.json: unsupported permission token(s): ${unknown.joinToString(", ")}"
        }
        manifest
    }

    /**
     * Validate the on-disk layout of an unpacked module directory.
     *
     * Currently enforces:
     *  * `module.json` is present and valid (re-uses [validateManifest]).
     *  * If [ModuleManifest.runtime] is non-blank, the referenced `.so` file
     *    exists inside [dir].
     *
     * Returns the validated manifest on success.
     */
    fun validatePackage(dir: File): Result<ModuleManifest> = runCatching {
        require(dir.exists() && dir.isDirectory) { "package dir does not exist: ${dir.absolutePath}" }
        val moduleJson = File(dir, "module.json")
        require(moduleJson.exists() && moduleJson.isFile) {
            "module.json missing in package dir ${dir.absolutePath}"
        }
        val manifest = validateManifest(moduleJson.readText()).getOrElse { throw it }
        if (manifest.runtime.isNotBlank()) {
            val runtime = File(dir, manifest.runtime)
            require(runtime.exists() && runtime.isFile) {
                "runtime artifact '${manifest.runtime}' missing in package dir"
            }
        }
        manifest
    }
}
