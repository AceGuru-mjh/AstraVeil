package com.astraveil.core.runtime

import kotlinx.serialization.Serializable

@Serializable
data class ProviderStatus(
    val name: String = "None",
    val version: String = "—",
    val score: Int = 0,
    val verified: Boolean = false,
    val capabilities: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
) {
    companion object {
        fun none() = ProviderStatus()
    }
}
