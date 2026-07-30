package com.astraveil.app.ipc

import android.content.Context
import com.astraveil.core.ipc.AstraDaemonClient
import com.astraveil.core.ipc.DaemonState
import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manages the connection lifecycle between the AstraVeil app and the
 * astrad daemon.
 *
 * The daemon is started externally (Magisk service.sh or init.rc).
 * This class only manages the CLIENT side: connect, monitor, reconnect.
 *
 * Usage:
 * ```
 * val dm = DaemonManager(context)
 * dm.connectWhenReady()   // call from Application.onCreate
 * dm.client.execute("id") // use from ViewModel/Repository
 * ```
 */
class DaemonManager(private val context: Context) {

    companion object {
        private const val TAG = "DaemonManager"
        private const val INITIAL_DELAY_MS = 3_000L
        private const val MAX_STARTUP_ATTEMPTS = 10
        private const val STARTUP_RETRY_INTERVAL_MS = 2_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The underlying IPC client. Exposed for ViewModel/Repository use. */
    val client = AstraDaemonClient()

    /** Observable connection state. */
    val state: StateFlow<DaemonState> get() = client.state

    /**
     * Attempt to connect to the daemon, retrying if it hasn't started yet.
     *
     * Called once from [com.astraveil.app.AstraVeilApplication.onCreate].
     * Non-blocking: runs on a background coroutine.
     */
    fun connectWhenReady() {
        scope.launch {
            // Give the daemon time to start (Magisk service.sh runs at boot)
            delay(INITIAL_DELAY_MS)

            for (attempt in 1..MAX_STARTUP_ATTEMPTS) {
                AstraLogger.i(TAG, "Connection attempt $attempt/$MAX_STARTUP_ATTEMPTS")
                val connected = client.connect()
                if (connected) {
                    AstraLogger.i(TAG, "Daemon connected on attempt $attempt")

                    // Verify with a ping
                    val pong = client.ping()
                    if (pong != null) {
                        AstraLogger.i(TAG, "Daemon ping OK: $pong")
                    }
                    return@launch
                }

                if (attempt < MAX_STARTUP_ATTEMPTS) {
                    delay(STARTUP_RETRY_INTERVAL_MS)
                }
            }

            AstraLogger.w(
                TAG,
                "Daemon not available after $MAX_STARTUP_ATTEMPTS attempts. " +
                    "App will run in local-only mode (no daemon IPC)."
            )
        }
    }

    /**
     * Execute a shell command via the daemon.
     * Returns null if daemon is not connected.
     */
    suspend fun execute(command: String): String? {
        if (client.state.value != DaemonState.ONLINE) return null
        return client.execute(command)
    }

    /** Disconnect and clean up. Call from Application.onTerminate or test teardown. */
    fun shutdown() {
        client.disconnect()
    }
}
