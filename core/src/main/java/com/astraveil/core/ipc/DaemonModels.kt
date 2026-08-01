package com.astraveil.core.ipc

import kotlinx.serialization.Serializable

/**
 * One capability with its detection source (matches daemon CapabilityInfo).
 */
@Serializable
data class DaemonCapabilityInfo(
    val available: Boolean = false,
    val source: String = "",
)

/**
 * Parses daemon's make_capability_response() with source:
 *   {"capabilities":{"root":{"available":true,"source":"getuid()==0"},...},"count":N}
 *
 * Provenance: ADVERTISED — this is what the daemon claims, not an
 * independent probe by the App.
 */
@Serializable
data class DaemonCapabilityResponse(
    val capabilities: Map<String, DaemonCapabilityInfo> = emptyMap(),
    val count: Int = 0,
)

/**
 * Parses one provider from daemon's make_providers_response().
 * Field names MUST match daemon json_codec.cpp ProviderInfo serialization.
 */
@Serializable
data class DaemonProviderInfo(
    val id: String = "",
    val name: String = "",
    val detected: Boolean = false,
    val available: Boolean = false,
    val version: String = "",
    val source: String = "",
)

/**
 * Parses daemon's make_providers_response():
 *   {"providers":[...],"count":N}
 */
@Serializable
data class DaemonProvidersResponse(
    val providers: List<DaemonProviderInfo> = emptyList(),
    val count: Int = 0,
)
