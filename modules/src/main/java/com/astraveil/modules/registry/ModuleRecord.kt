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
)
