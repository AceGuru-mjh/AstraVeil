package com.astraveil.core.runtime

import kotlinx.serialization.Serializable

@Serializable
data class CapabilityStatus(
    val name: String,
    val available: Boolean = false,
    val confidence: Int = 0,
    val tested: Boolean = false,
)
