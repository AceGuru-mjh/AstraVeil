package com.astraveil.core.capability.lease

import com.astraveil.core.event.AstraEvent
import com.astraveil.core.event.EventBus
import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the lifecycle of capability leases.
 *
 * 1. Issue leases with unique IDs and expiry times.
 * 2. Run a periodic sweeper that expires stale leases.
 * 3. Notify the daemon (via IPC) when a lease expires so PolicyBridge
 *    can deny subsequent execution attempts.
 * 4. Emit [LeaseEvent]s on the EventBus for UI observation.
 */
class LeaseManager(
    private val scope: CoroutineScope,
    private val eventBus: EventBus,
    private val sweepIntervalMs: Long = 10_000L,
) {
    private val leases = ConcurrentHashMap<String, CapabilityLease>()
    private val mutex = Mutex()
    private var sweepJob: Job? = null

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    suspend fun issue(
        moduleId: String,
        capability: String,
        durationMs: Long,
        renewable: Boolean = true,
        maxRenewals: Int = 3,
        reason: String = "",
    ): CapabilityLease = mutex.withLock {
        val now = System.currentTimeMillis()
        val lease = CapabilityLease(
            leaseId = UUID.randomUUID().toString(),
            moduleId = moduleId,
            capability = capability,
            grantedAtMs = now,
            expiresAtMs = if (durationMs == CapabilityLease.DURATION_PERMANENT) {
                Long.MAX_VALUE
            } else {
                now + durationMs
            },
            renewable = renewable,
            maxRenewals = maxRenewals,
            reason = reason,
        )
        leases[lease.leaseId] = lease
        _activeCount.value = countActive()
        AstraLogger.i(TAG, "Lease issued: ${lease.leaseId} " +
            "($moduleId/$capability, ${durationMs}ms, renewable=$renewable)")
        eventBus.emit(LeaseEvent.Issued(lease))
        lease
    }

    suspend fun renew(leaseId: String, additionalMs: Long): CapabilityLease? =
        mutex.withLock {
            val existing = leases[leaseId] ?: return@withLock null
            if (!existing.canRenew()) {
                AstraLogger.w(TAG, "Lease $leaseId cannot be renewed " +
                    "(count=${existing.renewalCount}, max=${existing.maxRenewals})")
                return@withLock null
            }
            val renewed = existing.renew(additionalMs)
            leases[leaseId] = renewed
            AstraLogger.i(TAG, "Lease renewed: $leaseId " +
                "(renewal ${renewed.renewalCount}/${renewed.maxRenewals})")
            eventBus.emit(LeaseEvent.Renewed(renewed))
            renewed
        }

    suspend fun revoke(leaseId: String): Boolean = mutex.withLock {
        val existing = leases[leaseId] ?: return@withLock false
        val revoked = existing.revoke()
        leases[leaseId] = revoked
        _activeCount.value = countActive()
        AstraLogger.i(TAG, "Lease revoked: $leaseId")
        eventBus.emit(LeaseEvent.Revoked(revoked))
        true
    }

    fun hasActiveLease(moduleId: String, capability: String): Boolean {
        val now = System.currentTimeMillis()
        return leases.values.any {
            it.moduleId == moduleId &&
            it.capability == capability &&
            it.isActive(now)
        }
    }

    fun leasesFor(moduleId: String): List<CapabilityLease> {
        val now = System.currentTimeMillis()
        return leases.values.filter {
            it.moduleId == moduleId && it.isActive(now)
        }
    }

    fun allActive(): List<CapabilityLease> {
        val now = System.currentTimeMillis()
        return leases.values.filter { it.isActive(now) }
            .sortedByDescending { it.expiresAtMs }
    }

    fun startSweeper() {
        sweepJob?.cancel()
        sweepJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(sweepIntervalMs)
                sweep()
            }
        }
    }

    fun stopSweeper() {
        sweepJob?.cancel()
        sweepJob = null
    }

    private suspend fun sweep() = mutex.withLock {
        val now = System.currentTimeMillis()
        val expired = leases.values.filter {
            !it.revoked && it.expiresAtMs != Long.MAX_VALUE && now >= it.expiresAtMs
        }
        for (lease in expired) {
            leases[lease.leaseId] = lease.copy(revoked = true)
            AstraLogger.i(TAG, "Lease expired: ${lease.leaseId} " +
                "(${lease.moduleId}/${lease.capability})")
            eventBus.emit(LeaseEvent.Expired(lease))
        }
        if (expired.isNotEmpty()) {
            _activeCount.value = countActive()
        }
    }

    private fun countActive(): Int {
        val now = System.currentTimeMillis()
        return leases.values.count { it.isActive(now) }
    }

    companion object {
        private const val TAG = "LeaseManager"
    }
}

sealed class LeaseEvent : AstraEvent {
    data class Issued(val lease: CapabilityLease) : LeaseEvent()
    data class Renewed(val lease: CapabilityLease) : LeaseEvent()
    data class Revoked(val lease: CapabilityLease) : LeaseEvent()
    data class Expired(val lease: CapabilityLease) : LeaseEvent()
}
