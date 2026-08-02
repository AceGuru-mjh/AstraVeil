package com.astraveil.core.capability.lease

import kotlinx.serialization.Serializable

/**
 * A time-bounded capability grant.
 *
 * Unlike Magisk/KernelSU's binary permission model, AstraVeil capabilities
 * can be leased for a finite duration. When the lease expires, the capability
 * is automatically revoked at the daemon level — the module's next execution
 * attempt will be denied by PolicyBridge.
 *
 * Inspired by DHCP leases and OAuth2 access tokens, applied to OS-level
 * capabilities for the first time in the Android root ecosystem.
 */
@Serializable
data class CapabilityLease(
    val leaseId: String,
    val moduleId: String,
    val capability: String,
    val grantedAtMs: Long,
    val expiresAtMs: Long,
    val renewable: Boolean = true,
    val maxRenewals: Int = 3,
    val renewalCount: Int = 0,
    val revoked: Boolean = false,
    val reason: String = "",
) {
    val durationMs: Long get() = expiresAtMs - grantedAtMs

    fun isActive(nowMs: Long = System.currentTimeMillis()): Boolean =
        !revoked && nowMs < expiresAtMs

    fun remainingMs(nowMs: Long = System.currentTimeMillis()): Long =
        if (isActive(nowMs)) expiresAtMs - nowMs else 0L

    fun canRenew(): Boolean = renewable && renewalCount < maxRenewals && !revoked

    fun renew(additionalMs: Long, nowMs: Long = System.currentTimeMillis()): CapabilityLease {
        require(canRenew()) { "Lease $leaseId cannot be renewed" }
        return copy(
            grantedAtMs = nowMs,
            expiresAtMs = nowMs + additionalMs,
            renewalCount = renewalCount + 1,
        )
    }

    fun revoke(): CapabilityLease = copy(revoked = true)

    companion object {
        val DURATION_5_MIN = 5 * 60 * 1000L
        val DURATION_15_MIN = 15 * 60 * 1000L
        val DURATION_1_HOUR = 60 * 60 * 1000L
        val DURATION_8_HOUR = 8 * 60 * 60 * 1000L
        val DURATION_24_HOUR = 24 * 60 * 60 * 1000L
        val DURATION_PERMANENT = Long.MAX_VALUE
    }
}
