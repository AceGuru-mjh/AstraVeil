package com.astraveil.core.ipc

import java.io.InputStream
import java.io.OutputStream

/**
 * Abstraction over the byte-stream transport used by [AstraDaemonClient].
 *
 * Extracting this interface (audit #5 V3) makes the client unit-testable:
 * a [FakeDaemonTransport] can be substituted in JVM tests without
 * requiring `android.net.LocalSocket` (which is only available on Android).
 *
 * The production implementation is [LocalSocketTransport] (Android only).
 * Tests use [FakeDaemonTransport] (pure JVM).
 *
 * Lifecycle:
 * ```
 * connect()          — open the transport
 *   ↓
 * outputStream / inputStream  — read/write frames
 *   ↓
 * close()            — release the transport
 * ```
 */
interface DaemonTransport {
    /** Open the connection. Throws on failure. */
    fun connect()

    /** Whether the transport is currently open. */
    val isOpen: Boolean

    /** Output stream for writing request frames. Valid after [connect]. */
    val outputStream: OutputStream

    /** Input stream for reading response frames. Valid after [connect]. */
    val inputStream: InputStream

    /** Close the transport. Idempotent. */
    fun close()
}
