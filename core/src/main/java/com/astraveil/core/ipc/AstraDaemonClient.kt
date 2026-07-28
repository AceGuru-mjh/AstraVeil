package com.astraveil.core.ipc

import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin client for the astrad Unix Domain Socket IPC protocol.
 *
 * Wire format: [4-byte big-endian length][payload]
 * Payload first byte = request type discriminator.
 *
 * This implementation uses LocalSocket (Android) or plain File I/O (JVM tests).
 */
class AstraDaemonClient(
    private val socketPath: String = "/dev/astra/astrad.sock",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    companion object {
        private const val TAG = "AstraDaemonClient"
        private const val MAX_FRAME_SIZE = 1 shl 20 // 1 MiB
        private const val CONNECT_TIMEOUT_MS = 5000L
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private const val MAX_RECONNECT_ATTEMPTS = 5

        // Request type bytes (must match daemon/src/main.cpp RequestType enum)
        const val TYPE_GET_CAPABILITY: Byte = 0x01
        const val TYPE_GET_PROVIDER: Byte = 0x02
        const val TYPE_EXECUTE: Byte = 0x03
        const val TYPE_PING: Byte = 0x04
        const val TYPE_GET_CAPABILITY_MATRIX: Byte = 0x05
        const val TYPE_INSTALL_MODULE: Byte = 0x06
        const val TYPE_REMOVE_MODULE: Byte = 0x07
        const val TYPE_START_MODULE: Byte = 0x08
        const val TYPE_STOP_MODULE: Byte = 0x09
        const val TYPE_LIST_MODULES: Byte = 0x0A
        const val TYPE_QUERY_PERMISSION: Byte = 0x0B
        const val TYPE_GRANT_PERMISSION: Byte = 0x0C
        const val TYPE_REVOKE_PERMISSION: Byte = 0x0D
        const val TYPE_GET_RISK_SCORE: Byte = 0x0E
        const val TYPE_GET_AUDIT_LOG: Byte = 0x0F
    }

    private val _state = MutableStateFlow(DaemonState.OFFLINE)
    val state: StateFlow<DaemonState> = _state.asStateFlow()

    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempts = 0

    /**
     * Connect to the daemon socket. Transitions state to CONNECTING → ONLINE or FAILED.
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        _state.value = DaemonState.CONNECTING
        try {
            val socket = android.net.LocalSocket()
            val address = android.net.LocalSocketAddress(
                socketPath,
                android.net.LocalSocketAddress.Namespace.FILESYSTEM
            )
            socket.connect(address)
            socket.soTimeout = 10_000

            outputStream = socket.outputStream
            inputStream = socket.inputStream
            _state.value = DaemonState.ONLINE
            reconnectAttempts = 0
            AstraLogger.i(TAG, "Connected to astrad at $socketPath")
            startHeartbeat()
            true
        } catch (e: Exception) {
            AstraLogger.e(TAG, "Connection failed: ${e.message}", e)
            _state.value = DaemonState.FAILED
            scheduleReconnect()
            false
        }
    }

    /**
     * Send a request frame and read the response.
     *
     * @param type Request type discriminator byte.
     * @param body UTF-8 body appended after the type byte.
     * @return Response payload (JSON string), or null on failure.
     */
    suspend fun request(type: Byte, body: String = ""): String? = withContext(Dispatchers.IO) {
        if (_state.value != DaemonState.ONLINE) {
            AstraLogger.w(TAG, "request() called while state=${_state.value}")
            return@withContext null
        }
        try {
            val payload = ByteArray(1 + body.length)
            payload[0] = type
            body.toByteArray(Charsets.UTF_8).copyInto(payload, 1)

            sendFrame(payload)
            val response = readFrame()
            if (response == null || response.isEmpty()) return@withContext null

            // First byte of response echoes the request type; rest is JSON
            String(response, 1, response.size - 1, Charsets.UTF_8)
        } catch (e: Exception) {
            AstraLogger.e(TAG, "request failed: ${e.message}", e)
            _state.value = DaemonState.FAILED
            scheduleReconnect()
            null
        }
    }

    /** Convenience: execute a shell command via the daemon. */
    suspend fun execute(command: String): String? = request(TYPE_EXECUTE, command)

    /** Convenience: get the live capability matrix JSON. */
    suspend fun getCapabilityMatrix(): String? = request(TYPE_GET_CAPABILITY_MATRIX)

    /** Convenience: ping the daemon. */
    suspend fun ping(): String? = request(TYPE_PING)

    /** Disconnect and clean up. */
    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        try { outputStream?.close() } catch (_: Exception) {}
        try { inputStream?.close() } catch (_: Exception) {}
        outputStream = null
        inputStream = null
        _state.value = DaemonState.OFFLINE
        AstraLogger.i(TAG, "Disconnected")
    }

    // ---- internal ----

    private fun sendFrame(payload: ByteArray) {
        val out = outputStream ?: throw IOException("Not connected")
        val header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
        header.putInt(payload.size)
        out.write(header.array())
        out.write(payload)
        out.flush()
    }

    private fun readFrame(): ByteArray? {
        val input = inputStream ?: return null
        val header = ByteArray(4)
        var read = 0
        while (read < 4) {
            val n = input.read(header, read, 4 - read)
            if (n <= 0) return null
            read += n
        }
        val length = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN).int
        if (length <= 0 || length > MAX_FRAME_SIZE) return null

        val buf = ByteArray(length)
        var got = 0
        while (got < length) {
            val n = input.read(buf, got, length - got)
            if (n <= 0) return null
            got += n
        }
        return buf
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _state.value == DaemonState.ONLINE) {
                delay(HEARTBEAT_INTERVAL_MS)
                val resp = ping()
                if (resp == null) {
                    AstraLogger.w(TAG, "Heartbeat failed, daemon may be dead")
                    _state.value = DaemonState.FAILED
                    scheduleReconnect()
                    break
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            AstraLogger.e(TAG, "Max reconnect attempts reached, giving up")
            return
        }
        reconnectAttempts++
        val delayMs = (1000L * reconnectAttempts).coerceAtMost(30_000L)
        scope.launch {
            delay(delayMs)
            AstraLogger.i(TAG, "Reconnect attempt $reconnectAttempts (delay=${delayMs}ms)")
            connect()
        }
    }
}
