package com.astraveil.modules.registry

import kotlinx.serialization.Serializable

@Serializable
enum class SignatureStatus {
    VERIFIED, UNSIGNED, UNKNOWN, INVALID,
}

@Serializable
data class ModuleRecord(
    val id: String,
    val version: String = "0.0.0",
    val apiVersion: Int = 1,
    val installPath: String,
    val state: String = "INSTALLED",
    val sourceHash: String? = null,
    val signatureStatus: SignatureStatus = SignatureStatus.UNKNOWN,
    val installSource: String = "",
    val grantedPermissions: Set<String> = emptySet(),
    val installTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    /**
     * Trust level recorded at install time — drives
     * [com.astraveil.modules.security.NativeModuleLoadPolicy] so the
     * runtime can refuse to load third-party native code in-process
     * until the isolated ModuleRunner (Phase 1) lands.
     *
     * Stored as a raw string (not the enum) so old persisted records
     * without the field still deserialize — `UNKNOWN_DEVELOPER` is a
     * safe default that forces the strictest gate.
     */
    val trustLevel: String = "UNSIGNED",
)
