package com.astraveil.modules.model

/**
 * Convert a legacy Phase-0 manifest (permissions as `List<String>`, name
 * used as id) into the canonical v3 [ModuleManifest].
 *
 * Used at the install boundary so the whole pipeline speaks one model,
 * regardless of whether the source manifest was Phase-0 or v3 format.
 * This is the fix for P1-7 ("preview succeeds, install fails"): both
 * paths convert to canonical BEFORE any validation/trust-gate logic, so
 * there is no second representation to drift from.
 */
fun legacyToCanonical(
    name: String,
    version: String,
    api: Int,
    permissions: List<String>,
    description: String = "",
    runtime: String = "",
    entry: String = "",
    minApi: Int = 1,
): ModuleManifest = ModuleManifest(
    id = name,                       // Phase-0 used name as id
    name = name,
    version = version,
    apiVersion = api,
    description = description,
    permissions = permissions.map { ModulePermission(capability = it) },
    requiredCapabilities = permissions,   // Phase-0 treated all as required
    runtime = runtime,
    entry = entry,
    minApi = minApi,
)

/**
 * Convert the existing v3 `modules.api.ModuleManifest` into the canonical
 * [ModuleManifest]. Field-for-field copy; no lossy conversion.
 */
fun com.astraveil.modules.api.ModuleManifest.toCanonical(): ModuleManifest = ModuleManifest(
    id = id,
    name = name,
    version = version,
    apiVersion = apiVersion,
    permissions = permissions.map { p ->
        ModulePermission(
            capability = p.capability,
            reason = p.reason,
            riskLevel = p.riskLevel,
        )
    },
)

/**
 * Convert the existing Phase-0 `modules.ModuleManifest` into the canonical
 * [ModuleManifest]. Permissions (List<String>) become capability-only
 * ModulePermission entries.
 */
fun com.astraveil.modules.ModuleManifest.toCanonical(): ModuleManifest = legacyToCanonical(
    name = name,
    version = version,
    api = api,
    permissions = permissions,
    description = description,
    runtime = runtime,
    entry = entry,
    minApi = minApi,
)
