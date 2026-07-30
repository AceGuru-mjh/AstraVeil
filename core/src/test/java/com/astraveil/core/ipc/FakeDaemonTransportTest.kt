package com.astraveil.core.ipc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FakeDaemonTransport].
 *
 * The fake is a pure-JVM [DaemonTransport] used by `AstraDaemonClient`
 * tests. It exposes:
 *  - [FakeDaemonTransport.sentFrames] for write-side assertions;
 *  - [FakeDaemonTransport.enqueueResponse] / [FakeDaemonTransport.prepareNextRead]
 *    for read-side programming;
 *  - standard `isOpen` lifecycle toggled by [DaemonTransport.connect] /
 *    [DaemonTransport.close].
 *
 * No Android dependency — runs in plain `src/test/`.
 */
class FakeDaemonTransportTest {

    @Test
    fun `connect_sets_isOpen_true`() {
        val transport = FakeDaemonTransport()
        assertFalse("fresh transport must not be open", transport.isOpen)
        transport.connect()
        assertTrue("connect() must set isOpen=true", transport.isOpen)
    }

    @Test
    fun `close_sets_isOpen_false`() {
        val transport = FakeDaemonTransport()
        transport.connect()
        assertTrue(transport.isOpen)
        transport.close()
        assertFalse("close() must set isOpen=false", transport.isOpen)
    }

    @Test
    fun `enqueueResponse_then_prepareNextRead_makes_data_available`() {
        val transport = FakeDaemonTransport()
        val response = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        transport.enqueueResponse(response)
        transport.prepareNextRead()

        val sink = ByteArray(response.size)
        val read = transport.inputStream.read(sink)
        assertEquals("every byte should be readable", response.size, read)
        assertArrayEquals(response, sink)
    }

    @Test
    fun `write_captures_sent_frame`() {
        val transport = FakeDaemonTransport()
        val frame = byteArrayOf(0x10, 0x20, 0x30)
        transport.outputStream.write(frame)
        transport.outputStream.flush()

        assertEquals("exactly one frame should be captured", 1, transport.sentFrames.size)
        assertArrayEquals(frame, transport.sentFrames[0])
    }

    @Test
    fun `multiple_writes_capture_multiple_frames`() {
        val transport = FakeDaemonTransport()
        val frame1 = byteArrayOf(0x01)
        val frame2 = byteArrayOf(0x02, 0x03)
        val frame3 = byteArrayOf(0x04, 0x05, 0x06)

        transport.outputStream.write(frame1)
        transport.outputStream.flush()
        transport.outputStream.write(frame2)
        transport.outputStream.flush()
        transport.outputStream.write(frame3)
        transport.outputStream.flush()

        assertEquals(3, transport.sentFrames.size)
        assertArrayEquals(frame1, transport.sentFrames[0])
        assertArrayEquals(frame2, transport.sentFrames[1])
        assertArrayEquals(frame3, transport.sentFrames[2])
    }
}
