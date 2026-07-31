package com.astraveil.app.hub

import kotlinx.serialization.Serializable

@Serializable
data class AstraHubIndex(
    val schemaVersion: Int,
    val updatedAt: String = "",
    val modules: List<HubModule> = emptyList(),
)

@Serializable
data class HubModule(
    val id: String,
    val name: String,
    val version: String,
    val apiVersion: Int = 3,
    val author: String = "",
    val description: String = "",
    val requiredCapabilities: List<String> = emptyList(),
    val optionalCapabilities: List<String> = emptyList(),
    val trustLevel: String = "UNKNOWN_DEVELOPER",
    val signatureFingerprint: String = "",
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long = 0,
    val publishedAt: String = "",
    val minAstraVeilVersion: String = "",
)
