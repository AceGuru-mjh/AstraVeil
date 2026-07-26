package com.astraveil.sdk.client

/**
 * v3 module-facing SDK client.
 *
 * Third-party modules never call `Runtime.getRuntime().exec("su")`.
 * They call [requestCapability] and AstraVeil brokers the request
 * through the permission engine → Rust policy → capability resolver →
 * provider, returning a boolean.
 *
 * This is the ecological moat: modules are capability *clients*, not
 * root processes.
 */
interface AstraModuleClient {

    /**
     * Request [capability] for this module. Returns true iff the
     * permission engine + Rust policy both allow it.
     */
    suspend fun requestCapability(capability: String): Boolean
}
