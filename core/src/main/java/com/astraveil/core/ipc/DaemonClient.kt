package com.astraveil.core.ipc

import com.astraveil.providers.ExecutionRequest
import com.astraveil.providers.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * Client-side entry to the AstraDaemon IPC.
 *
 * Connects to the daemon's Unix Domain Socket at
 * `/data/local/tmp/astrad.sock`. On Android this is opened via
 * `android.net.LocalSocket`; on a host JVM (for testing) we fall back to
 * a direct file-channel write so the API shape is exercisable.
 *
 * Frame format: `[4-byte big-endian length][payload bytes]` where the
 * payload is currently the text form `CAPABILITY:COMMAND` (the protobuf
 * ExecuteRequest lands once the Kotlin protobuf runtime is wired in).
 */
class DaemonClient(
    private val socketPath: String = "/data/local/tmp/astrad.sock",
) {

    /**
     * Execute [request] through the daemon. Returns the [ExecutionResult];
     * on any I/O failure returns a failed result with the error message.
     */
    suspend fun execute(request: ExecutionRequest): ExecutionResult =
        withContext(Dispatchers.IO) {
            val payload = "${request.capability}:${request.command}"
            val bytes = payload.toByteArray(Charsets.UTF_8)
            try {
                val frame = ByteArray(4 + bytes.size)
                // 4-byte big-endian length prefix.
                frame[0] = ((bytes.size ushr 24) and 0xFF).toByte()
                frame[1] = ((bytes.size ushr 16) and 0xFF).toByte()
                frame[2] = ((bytes.size ushr 8) and 0xFF).toByte()
                frame[3] = (bytes.size and 0xFF).toByte()
                System.arraycopy(bytes, 0, frame, 4, bytes.size)
                writeFrame(frame)
                ExecutionResult(success = true, output = "sent", error = null)
            } catch (t: Throwable) {
                ExecutionResult(
                    success = false,
                    output = "",
                    error = t.message ?: t.javaClass.simpleName,
                )
            }
        }

    /** Quick liveness check: is the socket file present? */
    fun connected(): Boolean = File(socketPath).exists()

    /**
     * Write a framed payload to the daemon socket.
     *
     * On Android this becomes `LocalSocket(AF_UNIX).connect(address)`;
     * on the host JVM we write through a RandomAccessFile so the call
     * path is exercised without an Android runtime.
     */
    private fun writeFrame(frame: ByteArray) {
        // TODO(Android): replace with android.net.LocalSocket.
        val raf = RandomAccessFile(socketPath, "rw")
        raf.use { it.write(frame) }
    }
}
