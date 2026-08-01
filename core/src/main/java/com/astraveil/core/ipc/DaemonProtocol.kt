package com.astraveil.core.ipc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wire protocol between the App and astrad.
 *
 * Frame layout (matches daemon/src/ipc/socket_server.cpp):
 *   [4-byte big-endian payload length][1-byte type][body bytes]
 *   payload length = 1 (type) + body.size
 *
 * Response frame:
 *   [4-byte big-endian length][1-byte echo type][JSON body bytes]
 *
 * IMPORTANT (audit P1-14): length is computed from UTF-8 BYTES, never
 * String.length. Non-ASCII (e.g. Chinese) would otherwise corrupt frames.
 *
 * IMPORTANT (audit P0-1): Execute requests MUST be structured
 * [ExecuteRequest] JSON — raw command strings are never the public IPC API.
 */
object DaemonProtocol {

    // Request types — MUST match daemon/src/main.cpp RequestType enum.
    const val TYPE_PING: Byte = 0x04
    const val TYPE_EXECUTE: Byte = 0x03
    const val TYPE_GET_CAPABILITY: Byte = 0x01
    const val TYPE_GET_PROVIDER: Byte = 0x02
    const val TYPE_GET_CAPABILITY_MATRIX: Byte = 0x05
    const val TYPE_INSTALL_MODULE: Byte = 0x06
    const val TYPE_REMOVE_MODULE: Byte = 0x07
    const val TYPE_START_MODULE: Byte = 0x08
    const val TYPE_STOP_MODULE: Byte = 0x09
    const val TYPE_LIST_MODULES: Byte = 0x0A
    const val TYPE_GET_AUDIT_LOG: Byte = 0x0F

    const val MAX_FRAME_BYTES = 8 * 1024 * 1024   // 8MB safety cap

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Structured execute request (audit P0-1). The daemon runs this through
     * PolicyBridge.checkWith(moduleId, capability, risk, approval) — raw
     * command strings are never the public IPC API.
     */
    @Serializable
    data class ExecuteRequest(
        val moduleId: String,
        val capability: String,
        val riskLevel: Int,
        val approved: Boolean,
        val caller: String,
        val command: String,
    )

    /** Daemon response for Execute / generic calls. */
    @Serializable
    data class DaemonResponse(
        val success: Boolean = false,
        val exit_code: Int = -1,
        val stdout: String = "",
        val stderr: String = "",
        val error: String? = null,
        val reason: String? = null,
    ) {
        val denied: Boolean get() = error == "policy_denied"
        val needsApproval: Boolean get() = error == "approval_required"
    }

    // ── Frame encoding ──

    /** Encode a request into a length-prefixed frame. */
    fun encodeFrame(type: Byte, body: String): ByteArray {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)   // P1-14: bytes, not length
        val payload = ByteArray(1 + bodyBytes.size)
        payload[0] = type
        bodyBytes.copyInto(payload, 1)
        require(payload.size <= MAX_FRAME_BYTES) { "frame too large: ${payload.size}" }
        val header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
            .putInt(payload.size).array()
        return header + payload
    }

    /** Decode a response payload — caller already read the 4-byte length. */
    fun decodeResponse(payload: ByteArray): String {
        // First byte echoes the request type; rest is JSON
        val jsonStart = if (payload.isNotEmpty()) 1 else 0
        return String(payload, jsonStart, payload.size - jsonStart, Charsets.UTF_8)
    }
}
