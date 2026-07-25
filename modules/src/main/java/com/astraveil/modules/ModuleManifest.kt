package com.astraveil.modules

import kotlinx.serialization.Serializable

/**
 * Deserialised contents of the `module.json` file shipped at the root of every
 * `.avm` package.
 *
 * `module.json` is THE contract between a third-party module author and the
 * AstraVeil runtime. The fields below are validated by
 * [ModuleValidator.validateManifest] at install time; any module that fails
 * validation is rejected before it is unpacked.
 *
 * Field semantics:
 *
 *  * [name] — module id, lowercase ASCII, dotted form recommended
 *    (e.g. `"com.example.coolmod"`). MUST be unique across installed modules.
 *  * [version] — arbitrary version string, surfaced in UI.
 *  * [api] — Module API level the module was written against. Must be `<=`
 *    [com.astraveil.sdk.AstraSdkConstants.MODULE_API_LEVEL].
 *  * [author] — optional human-readable author string.
 *  * [description] — optional one-line description for UI.
 *  * [permissions] — list of permission tokens requested from
 *    [com.astraveil.sdk.AstraSdkConstants.SUPPORTED_PERMISSIONS]. Granted at
 *    install time, stored on the [AstraModule] instance.
 *  * [runtime] — relative path inside the .avm package to the native runtime
 *    shared library (e.g. `runtime/arm64.so`).
 *  * [entry] — C-exported symbol called by [ModuleRuntime] when the module is
 *    started. Signature: `extern "C" int <entry>(AstraHandle*)`.
 *  * [minApi] — minimum Module API level required for the module to function
 *    correctly. AstraVeil will refuse to install on older runtimes.
 */
@Serializable
data class ModuleManifest(
    val name: String,
    val version: String,
    val api: Int = 1,
    val author: String = "",
    val description: String = "",
    val permissions: List<String> = emptyList(),
    val runtime: String = "",
    val entry: String = "",
    val minApi: Int = 1
) {
    companion object {
        /** Sentinel used when a manifest could not be parsed. */
        fun empty(): ModuleManifest = ModuleManifest(name = "", version = "")
    }
}
