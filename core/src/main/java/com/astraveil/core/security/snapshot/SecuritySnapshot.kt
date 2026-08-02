package com.astraveil.core.security.snapshot

import com.astraveil.core.capability.lease.CapabilityLease
import com.astraveil.core.logger.AstraLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * A complete snapshot of AstraVeil's security state at a point in time.
 *
 * Captures the full authorization state so it can be restored later:
 * 1. All permission grants (module → capability → granted)
 * 2. All explicit denials
 * 3. All active capability leases (Innovation 1)
 * 4. The audit chain head hash (Innovation 11) for integrity verification
 * 5. The active root provider ID
 * 6. Module states (installed/running/stopped)
 *
 * Use cases:
 * - "Before installing module X" → snapshot → install → if broken → rollback
 * - "Before lockdown" → snapshot → lockdown → if too restrictive → rollback
 * - "Before provider switch" → snapshot → switch → if unstable → rollback
 *
 * This is analogous to database SAVEPOINT / ROLLBACK TO SAVEPOINT,
 * or ZFS/Btrfs filesystem snapshots, applied to OS security policy state.
 * No existing Android root tool provides security state rollback.
 */
@Serializable
data class SecuritySnapshot(
    val snapshotId: String,
    val name: String,
    val createdAtMs: Long,
    val reason: String,

    /** Permission grants: moduleId → set of granted capabilities. */
    val permissionGrants: Map<String, Set<String>>,

    /** Permission denials: moduleId → set of explicitly denied capabilities. */
    val permissionDenials: Map<String, Set<String>>,

    /** Active capability leases at snapshot time. */
    val activeLeases: List<CapabilityLease>,

    /** The audit chain head hash at snapshot time (Innovation 11). */
    val auditChainHead: String,

    /** The active root provider ID. */
    val activeProviderId: String,

    /** Module states: moduleId → state string ("installed"/"running"/"stopped"). */
    val moduleStates: Map<String, String>,

    /** AstraVeil version at snapshot time. */
    val astraveilVersion: String,
) {
    /** Human-readable summary. */
    fun summary(): String = buildString {
        append("'$name'")
        append(" @ ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()).format(java.util.Date(createdAtMs))}")
        append(" — ${permissionGrants.size} modules")
        append(", ${activeLeases.size} leases")
        append(", provider=$activeProviderId")
        if (reason.isNotEmpty()) append(" ($reason)")
    }
}

/**
 * Result of comparing two snapshots.
 */
data class SnapshotDiff(
    val changes: List<String>,
    val permissionsAdded: Map<String, Set<String>>,
    val permissionsRemoved: Map<String, Set<String>>,
    val providerChanged: Boolean,
    val leasesAdded: Int,
    val leasesRemoved: Int,
) {
    val hasChanges: Boolean get() = changes.isNotEmpty()
}

/**
 * Manages creation, listing, comparison, and restoration of security snapshots.
 *
 * Thread safety: all operations are synchronized on the internal list.
 * Snapshots are stored in memory; for persistence, use [export]/[import].
 */
class SnapshotManager {

    private val snapshots = mutableListOf<SecuritySnapshot>()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /** Maximum number of snapshots to retain (oldest are evicted). */
    var maxSnapshots: Int = 50

    /**
     * Create a new snapshot of the current security state.
     *
     * @param name User-visible name (e.g. "Before installing FontMod").
     * @param reason Why this snapshot was created.
     * @param permissionGrants Current permission grants.
     * @param permissionDenials Current explicit denials.
     * @param activeLeases Current active leases (from LeaseManager.allActive()).
     * @param auditChainHead Current audit chain head hash (from AuditChain.headHash).
     * @param activeProviderId Current active provider ID.
     * @param moduleStates Current module states.
     * @param version Current AstraVeil version.
     * @return The created snapshot.
     */
    @Synchronized
    fun create(
        name: String,
        reason: String,
        permissionGrants: Map<String, Set<String>>,
        permissionDenials: Map<String, Set<String>>,
        activeLeases: List<CapabilityLease>,
        auditChainHead: String,
        activeProviderId: String,
        moduleStates: Map<String, String>,
        version: String,
    ): SecuritySnapshot {
        val snapshot = SecuritySnapshot(
            snapshotId = UUID.randomUUID().toString(),
            name = name,
            createdAtMs = System.currentTimeMillis(),
            reason = reason,
            permissionGrants = permissionGrants,
            permissionDenials = permissionDenials,
            activeLeases = activeLeases,
            auditChainHead = auditChainHead,
            activeProviderId = activeProviderId,
            moduleStates = moduleStates,
            astraveilVersion = version,
        )

        snapshots.add(snapshot)

        // Evict oldest if over limit.
        while (snapshots.size > maxSnapshots) {
            val evicted = snapshots.removeAt(0)
            AstraLogger.w(TAG, "Evicted oldest snapshot '${evicted.name}' (limit=$maxSnapshots)")
        }

        AstraLogger.i(TAG, "Snapshot created: ${snapshot.summary()}")
        return snapshot
    }

    /**
     * List all snapshots (newest first).
     */
    @Synchronized
    fun list(): List<SecuritySnapshot> =
        snapshots.sortedByDescending { it.createdAtMs }

    /**
     * Get a snapshot by ID.
     */
    @Synchronized
    fun get(snapshotId: String): SecuritySnapshot? =
        snapshots.find { it.snapshotId == snapshotId }

    /**
     * Get the most recent snapshot.
     */
    @Synchronized
    fun latest(): SecuritySnapshot? =
        snapshots.maxByOrNull { it.createdAtMs }

    /**
     * Delete a snapshot.
     */
    @Synchronized
    fun delete(snapshotId: String): Boolean {
        val removed = snapshots.removeAll { it.snapshotId == snapshotId }
        if (removed) AstraLogger.i(TAG, "Snapshot deleted: $snapshotId")
        return removed
    }

    /**
     * Compute the diff between two snapshots.
     *
     * @param oldId The baseline snapshot ID.
     * @param newId The comparison snapshot ID.
     * @return A structured diff describing what changed.
     */
    @Synchronized
    fun diff(oldId: String, newId: String): SnapshotDiff? {
        val old = get(oldId) ?: return null
        val new = get(newId) ?: return null

        val changes = mutableListOf<String>()
        val permissionsAdded = mutableMapOf<String, Set<String>>()
        val permissionsRemoved = mutableMapOf<String, Set<String>>()

        // Permission changes.
        val allModules = (old.permissionGrants.keys + new.permissionGrants.keys).toSet()
        for (mod in allModules) {
            val oldCaps = old.permissionGrants[mod] ?: emptySet()
            val newCaps = new.permissionGrants[mod] ?: emptySet()
            val added = newCaps - oldCaps
            val removed = oldCaps - newCaps
            if (added.isNotEmpty()) {
                changes.add("+$mod: gained ${added.joinToString()}")
                permissionsAdded[mod] = added
            }
            if (removed.isNotEmpty()) {
                changes.add("-$mod: lost ${removed.joinToString()}")
                permissionsRemoved[mod] = removed
            }
        }

        // Provider change.
        val providerChanged = old.activeProviderId != new.activeProviderId
        if (providerChanged) {
            changes.add("Provider: ${old.activeProviderId} → ${new.activeProviderId}")
        }

        // Lease changes.
        val oldLeaseIds = old.activeLeases.map { it.leaseId }.toSet()
        val newLeaseIds = new.activeLeases.map { it.leaseId }.toSet()
        val leasesAdded = (newLeaseIds - oldLeaseIds).size
        val leasesRemoved = (oldLeaseIds - newLeaseIds).size
        if (leasesAdded > 0) changes.add("+$leasesAdded new leases")
        if (leasesRemoved > 0) changes.add("-$leasesRemoved expired/revoked leases")

        // Module state changes.
        val allModuleIds = (old.moduleStates.keys + new.moduleStates.keys).toSet()
        for (modId in allModuleIds) {
            val oldState = old.moduleStates[modId]
            val newState = new.moduleStates[modId]
            if (oldState != newState) {
                changes.add("Module $modId: ${oldState ?: "absent"} → ${newState ?: "absent"}")
            }
        }

        return SnapshotDiff(
            changes = changes,
            permissionsAdded = permissionsAdded,
            permissionsRemoved = permissionsRemoved,
            providerChanged = providerChanged,
            leasesAdded = leasesAdded,
            leasesRemoved = leasesRemoved,
        )
    }

    /**
     * Export all snapshots as JSON (for backup).
     */
    @Synchronized
    fun export(): String = json.encodeToString(snapshots)

    /**
     * Import snapshots from JSON.
     *
     * @return Number of snapshots imported.
     */
    @Synchronized
    fun import(jsonStr: String): Int {
        val imported: List<SecuritySnapshot> = try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            AstraLogger.e(TAG, "Failed to parse snapshots JSON: ${e.message}", e)
            return 0
        }
        snapshots.clear()
        snapshots.addAll(imported)
        AstraLogger.i(TAG, "Imported ${imported.size} snapshots")
        return imported.size
    }

    companion object {
        private const val TAG = "SnapshotManager"
    }
}
