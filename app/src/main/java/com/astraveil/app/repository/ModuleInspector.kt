package com.astraveil.app.repository

import android.content.Context
import android.net.Uri
import com.astraveil.core.modules.manifest.AvmManifestParser

/**
 * High-level facade over [AvmManifestParser] for the install-preview flow
 * (Patch 18.2.2).
 *
 * Where [AvmManifestParser] is a low-level, JVM-only ZIP+JSON engine living
 * in `:core`, [ModuleInspector] is the `:app`-side component the ViewModel
 * actually calls. It owns the Android `ContentResolver` interaction and
 * projects the parsed manifest into the UI-friendly [ModulePreview] shape.
 *
 * Contract:
 * ```
 * ViewModel.previewUri(uri)
 *   → ModuleRepository.preview(uri)
 *     → ModuleInspector.inspect(context, uri)
 *       → ContentResolver.openInputStream(uri)
 *       → AvmManifestParser.parse(stream)      // :core engine
 *       → map → ModulePreview
 * ```
 *
 * Risk data flows straight from the manifest. No heuristics, no defaults.
 */
object ModuleInspector {

    /**
     * Inspect a `.avm` package referenced by a content [uri] WITHOUT
     * installing it. Only `module.json` is read from the ZIP.
     */
    suspend fun inspect(context: Context, uri: Uri): InspectionResult {
        val parserResult: AvmManifestParser.PreviewResult = try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return InspectionResult.Failure(
                    AvmManifestParser.PreviewError.IO_ERROR.message
                )
            stream.use { AvmManifestParser.parse(it) }
        } catch (e: Exception) {
            return InspectionResult.Failure(
                AvmManifestParser.PreviewError.IO_ERROR.message
            )
        }

        return when (parserResult) {
            is AvmManifestParser.PreviewResult.Success ->
                InspectionResult.Success(parserResult.module.toPreview())
            is AvmManifestParser.PreviewResult.Failure ->
                InspectionResult.Failure(parserResult.reason.message)
        }
    }
}

/**
 * Outcome of [ModuleInspector.inspect].
 */
sealed class InspectionResult {
    data class Success(val preview: ModulePreview) : InspectionResult()
    data class Failure(val reason: String) : InspectionResult()
}

/**
 * Pre-install preview of a `.avm` package — the data the confirmation
 * dialog renders. Mirrors [com.astraveil.core.modules.model.ModuleInfo]
 * but is the dedicated preview type so the install flow never confuses
 * "what we are about to install" with "what is already installed".
 */
data class ModulePreview(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val permissions: List<PermissionPreview>,
)

/**
 * A single requested permission in a [ModulePreview].
 *
 * [risk] is `null` when the manifest did not declare a risk level
 * (Phase-0 string-only permissions). The UI renders this as "Unknown".
 */
data class PermissionPreview(
    val capability: String,
    val risk: Int?,
    val reason: String,
)

/** Map the :core [com.astraveil.core.modules.model.ModuleInfo] to the preview type. */
private fun com.astraveil.core.modules.model.ModuleInfo.toPreview(): ModulePreview = ModulePreview(
    id = id,
    name = name,
    version = version,
    description = description,
    permissions = permissions.map { p ->
        PermissionPreview(capability = p.capability, risk = p.risk, reason = p.reason)
    },
)
