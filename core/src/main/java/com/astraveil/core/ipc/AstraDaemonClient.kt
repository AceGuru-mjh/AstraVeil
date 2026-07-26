package com.astraveil.core.ipc

import com.astraveil.providers.ExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Phase 4 public client the AstraUI uses to talk to astrad.
 *
 * Wraps [DaemonClient] (the raw socket transport) with a [DaemonState]
 * flow so the UI can render OFFLINE → CONNECTING → ONLINE transitions.
 * The [execute] path sends a capability-tagged request and returns the
 * daemon's [ExecutionResult].
 */
interface AstraDaemonClient {

    /** Live daemon connection state for the UI. */
    val state: StateFlow<DaemonState>

    /** Connect to the daemon socket. Updates [state]. */
    suspend fun connect(): Boolean

    /** Execute @p capability via the daemon. Returns the result. */
    suspend fun execute(capability: String): ExecutionResult

    /** Send a heartbeat; returns true iff the daemon responded alive. */
    suspend fun heartbeat(): Boolean
}

/**
 * Default [AstraDaemonClient] backed by [DaemonClient].
 */
class DefaultAstraDaemonClient(
    private val transport: DaemonClient = DaemonClient(),
) : AstraDaemonClient {

    private val _state = MutableStateFlow(DaemonState.OFFLINE)
    override val state: StateFlow<DaemonState> = _state.asStateFlow()

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        _state.value = DaemonState.CONNECTING
        val ok = transport.connected()
        _state.value = if (ok) DaemonState.ONLINE else DaemonState.FAILED
        ok
    }

    override suspend fun execute(capability: String): ExecutionResult =
        withContext(Dispatchers.IO) {
            if (_state.value != DaemonState.ONLINE) {
                return@withContext ExecutionResult(
                    success = false,
                    output = "",
                    error = "daemon offline",
                )
            }
            // Phase 4: the command field is unused by the current text
            // frame; the daemon routes by capability.
            transport.execute(
                com.astraveil.providers.ExecutionRequest(
                    moduleId = "astra:builtin",
                    capability = capability,
                    command = "",
                )
            )
        }

    override suspend fun heartbeat(): Boolean = withContext(Dispatchers.IO) {
        // Phase 4: a real heartbeat sends a HeartbeatRequest proto frame.
        // For now, liveness = socket file present.
        transport.connected()
    }
}
