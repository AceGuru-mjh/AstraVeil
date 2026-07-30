package com.astraveil.core.ipc

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayDeque

/**
 * Pure-JVM [DaemonTransport] for unit tests (audit #5 V3).
 *
 * Holds a queue of pre-programmed response frames. Each call to
 * [readFrame] (via the input stream) pops the next response. Writes
 * (via the output stream) are captured in a buffer so tests can assert
 * on the request frames the client sent.
 *
 * Usage:
 * ```
 * val transport = FakeDaemonTransport()
 * transport.enqueueResponse(byteArrayOf(0x04) + "\"pong\"".toByteArray())
 * val client = AstraDaemonClient(transport = transport)
 * client.ping()
 * assertEquals(1, transport.sentFrames.size)
 * ```
 *
 * No Android dependency — runs in plain JVM `src/test/`.
 */
class FakeDaemonTransport : DaemonTransport {

    /** Frames the client has sent (for test assertions). */
    val sentFrames = mutableListOf<ByteArray>()

    /** Queue of responses to return on subsequent reads. */
    private val responseQueue = ArrayDeque<ByteArray>()

    private var open = false
    private val writeBuffer = ByteArrayOutputStream()
    private var readBuffer = ByteArrayInputStream(ByteArray(0))

    override var isOpen: Boolean = false
        private set

    override val outputStream: OutputStream = object : OutputStream() {
        override fun write(b: Int) {
            writeBuffer.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            writeBuffer.write(b, off, len)
        }

        override fun flush() {
            val frame = writeBuffer.toByteArray()
            if (frame.isNotEmpty()) {
                sentFrames.add(frame.copyOf())
                writeBuffer.reset()
            }
        }
    }

    override val inputStream: InputStream = object : InputStream() {
        override fun read(): Int = readBuffer.read()
    }

    override fun connect() {
        isOpen = true
        open = true
    }

    override fun close() {
        isOpen = false
        open = false
    }

    /** Pre-program a response frame to be returned on the next read. */
    fun enqueueResponse(frame: ByteArray) {
        responseQueue.add(frame.copyOf())
    }

    /**
     * Called by the client's readFrame() before reading: swaps in the
     * next pre-programmed response.
     */
    fun prepareNextRead() {
        val next = responseQueue.pollFirst()
        readBuffer = if (next != null) ByteArrayInputStream(next) else ByteArrayInputStream(ByteArray(0))
    }
}
