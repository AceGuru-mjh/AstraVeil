package com.astraveil.providers

/**
 * v3 summary of a registered provider — id + display name + advertised
 * capability set. Used by the UI and the capability resolver to render
 * the provider catalogue without holding live provider instances.
 */
data class ProviderInfo(
    val id: String,
    val name: String,
    val capabilities: Set<ProviderCapability>,
)
