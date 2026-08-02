package com.astraveil.core.security.audit

import com.astraveil.core.logger.AstraLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Immutable hash-chained audit log for capability operations.
 *
 * Each entry contains the SHA-256 hash of the previous entry, forming
 * a tamper-evident chain. Modifying any entry breaks the chain from
 * that point forward, making tampering detectable.
 *
 * This is a SINGLE-NODE hash chain (not a distributed blockchain).
 * It provides tamper EVIDENCE, not tamper PREVENTION. For prevention,
 * the chain head can be periodically anchored to:
 * 1. An eBPF BPF_MAP_TYPE_ARRAY (kernel memory — Innovation 4, Phase 1)
 * 2. Android Keystore (hardware-backed — Innovation 5 extension, Phase 1)
 *
 * Analogy: Certificate Transparency logs use Merkle Trees to make
 * certificate issuance tamper-evident. AstraVeil uses a hash chain
 * to make root operations tamper-evident.
 *
 * Integration with existing AuditLogger:
 * The daemon's C++ AuditLogger writes to /data/astra/log/security.log.
 * This Kotlin-side AuditChain is the APP-LEVEL audit trail that records
 * capability grants, lease operations, and policy decisions. The two
 * are complementary: daemon log = execution audit, chain = policy audit.
 */
class AuditChain {

    /** A single link in the audit chain. */
    @Serializable
    data class AuditLink(
        val sequence: Long,
        val timestampMs: Long,
        val eventType: String,
        val moduleId: String,
        val capability: String,
        val detail: String,
        val prevHash: String,
        val hash: String,
    )

    private val chain = mutableListOf<AuditLink>()
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    /** The hash of the genesis (first) link, or all-zeros if empty. */
    val genesisHash: String
        get() = chain.firstOrNull()?.hash ?: GENESIS_HASH

    /** The hash of the most recent link (chain head). */
    val headHash: String
        get() = chain.lastOrNull()?.hash ?: GENESIS_HASH

    /** Number of links in the chain. */
    val size: Int get() = chain.size

    /**
     * Append a new audit entry to the chain.
     *
     * The hash is computed over: sequence|timestamp|eventType|moduleId|
     * capability|detail|prevHash. This ensures that modifying ANY field
     * in ANY link invalidates all subsequent links.
     *
     * @param eventType The operation type (e.g. "GRANT", "REVOKE", "EXECUTE",
     *                  "LEASE_ISSUE", "LEASE_EXPIRE", "PROVIDER_SWITCH",
     *                  "POLICY_DECISION", "MODULE_INSTALL").
     * @param moduleId The module involved.
     * @param capability The capability involved.
     * @param detail Additional context (command, reason, risk level, etc.).
     * @return The created link.
     */
    @Synchronized
    fun append(
        eventType: String,
        moduleId: String,
        capability: String,
        detail: String = "",
    ): AuditLink {
        val prevHash = headHash
        val sequence = chain.size.toLong()
        val timestampMs = System.currentTimeMillis()

        val payload = buildHashPayload(sequence, timestampMs, eventType,
            moduleId, capability, detail, prevHash)
        val hash = sha256Hex(payload)

        val link = AuditLink(
            sequence = sequence,
            timestampMs = timestampMs,
            eventType = eventType,
            moduleId = moduleId,
            capability = capability,
            detail = detail,
            prevHash = prevHash,
            hash = hash,
        )

        chain.add(link)

        if (chain.size % LOG_INTERVAL == 0) {
            AstraLogger.i(TAG, "Audit chain: ${chain.size} links, head=$hash")
        }

        return link
    }

    /**
     * Verify the integrity of the entire chain.
     *
     * Walks from genesis to head, recomputing each hash and checking:
     * 1. The stored hash matches the recomputed hash.
     * 2. The prevHash field matches the previous link's hash.
     *
     * @return The index of the first broken link, or -1 if intact.
     */
    @Synchronized
    fun verify(): Int {
        for (i in chain.indices) {
            val link = chain[i]

            // Check prevHash linkage.
            val expectedPrevHash = if (i == 0) GENESIS_HASH else chain[i - 1].hash
            if (link.prevHash != expectedPrevHash) {
                AstraLogger.e(TAG, "Chain broken at index $i: prevHash mismatch", null)
                return i
            }

            // Recompute hash.
            val payload = buildHashPayload(link.sequence, link.timestampMs,
                link.eventType, link.moduleId, link.capability,
                link.detail, link.prevHash)
            val computedHash = sha256Hex(payload)
            if (computedHash != link.hash) {
                AstraLogger.e(TAG, "Chain broken at index $i: hash mismatch " +
                    "(stored=${link.hash}, computed=$computedHash)", null)
                return i
            }
        }
        return -1  // Chain is intact.
    }

    /**
     * Verify only the last N links (faster for periodic checks).
     *
     * @param n Number of links from the tail to verify.
     * @return true if the tail is intact.
     */
    @Synchronized
    fun verifyTail(n: Int): Boolean {
        val start = maxOf(0, chain.size - n)
        for (i in start until chain.size) {
            val link = chain[i]
            val expectedPrevHash = if (i == 0) GENESIS_HASH else chain[i - 1].hash
            if (link.prevHash != expectedPrevHash) return false

            val payload = buildHashPayload(link.sequence, link.timestampMs,
                link.eventType, link.moduleId, link.capability,
                link.detail, link.prevHash)
            if (sha256Hex(payload) != link.hash) return false
        }
        return true
    }

    /**
     * Export the chain as a JSON array (for backup/export).
     */
    @Synchronized
    fun export(): String = json.encodeToString(chain)

    /**
     * Import a chain from JSON and verify its integrity.
     *
     * @return true if the imported chain is valid and intact.
     */
    @Synchronized
    fun import(jsonStr: String): Boolean {
        val imported: List<AuditLink> = try {
            json.decodeFromString(jsonStr)
        } catch (e: Exception) {
            AstraLogger.e(TAG, "Failed to parse audit chain JSON: ${e.message}", e)
            return false
        }

        chain.clear()
        chain.addAll(imported)
        val brokenAt = verify()
        if (brokenAt >= 0) {
            AstraLogger.e(TAG, "Imported chain is broken at index $brokenAt", null)
            chain.clear()
            return false
        }
        AstraLogger.i(TAG, "Imported audit chain: ${chain.size} links, intact")
        return true
    }

    /**
     * Get the last N links (for UI display).
     */
    @Synchronized
    fun tail(n: Int): List<AuditLink> = chain.takeLast(n)

    /**
     * Get all links for a specific module.
     */
    @Synchronized
    fun forModule(moduleId: String): List<AuditLink> =
        chain.filter { it.moduleId == moduleId }

    /**
     * Get all links of a specific event type.
     */
    @Synchronized
    fun forEventType(eventType: String): List<AuditLink> =
        chain.filter { it.eventType == eventType }

    private fun buildHashPayload(
        sequence: Long,
        timestampMs: Long,
        eventType: String,
        moduleId: String,
        capability: String,
        detail: String,
        prevHash: String,
    ): String = "$sequence|$timestampMs|$eventType|$moduleId|$capability|$detail|$prevHash"

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "AuditChain"
        private const val LOG_INTERVAL = 100
        private val GENESIS_HASH = "0".repeat(64)
    }
}
