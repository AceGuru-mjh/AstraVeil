package com.astraveil.core.execution

import com.astraveil.app.AstraVeilApplication
import com.astraveil.core.ipc.DaemonState
import com.astraveil.core.logger.AstraLogger

/**
 * Unified execution entry point (audit P1-11).
 *
 * THE single execution truth for all NON-interactive command execution:
 *   ExecutionRouter → daemon IPC → PolicyBridge (Rust, fail-closed) → executor
 *
 * There is deliberately NO direct-su fallback for module/programmatic
 * execution — that duplicate path is removed by design. If the daemon is
 * offline, execution is REFUSED, not silently rerouted to `su -c`.
 *
 * The interactive terminal is a separate, explicitly-approved channel
 * (TrustedInteractiveSession, audit P1-12) and does NOT use this router.
 *
 * NOTE: lives in :app (not :core) because it references
 * [com.astraveil.app.AstraVeilApplication.daemonManager] from :app.
 */
object ExecutionRouter {

    private const val TAG = "ExecutionRouter"

    /** Which path an execution took (provenance, visible to UI). */
    enum class Path {
        DAEMON,               // unified, policy-enforced
        REFUSED_NO_DAEMON,    // daemon offline → refused (no su fallback)
        REFUSED_POLICY,       // daemon online, Rust policy denied
        REFUSED_NEEDS_APPROVAL,
    }

    data class Result(
        val success: Boolean,
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val path: Path,
        val message: String = "",
    ) {
        val executed: Boolean get() = path == Path.DAEMON
    }

    /**
     * Execute a command on behalf of a module / programmatic caller.
     * Daemon-only. Refuses if daemon offline or policy denies.
     */
    suspend fun executeForModule(
        moduleId: String,
        capability: String,
        riskLevel: Int,
        approved: Boolean,
        command: String,
    ): Result {
        val client = AstraVeilApplication.daemonManager.client

        // ① daemon must be online — offline = refuse, no su fallback
        if (client.state.value != DaemonState.ONLINE) {
            AstraLogger.w(TAG, "refused (no daemon): module=$moduleId cmd=$command")
            return Result(
                success = false, stdout = "", stderr = "",
                exitCode = -1, path = Path.REFUSED_NO_DAEMON,
                message = "astrad offline — module execution requires the daemon " +
                    "(unified execution, audit P1-11). Start astrad and retry.",
            )
        }

        // ② structured request → daemon → PolicyBridge decision
        val resp = client.executeStructured(
            command = command,
            moduleId = moduleId,
            capability = capability,
            riskLevel = riskLevel,
            approved = approved,
            caller = "module:$moduleId",
        )

        return when {
            resp.denied -> {
                AstraLogger.w(TAG, "policy denied: module=$moduleId cap=$capability")
                Result(false, "", resp.reason ?: "policy denied",
                    -1, Path.REFUSED_POLICY,
                    "Rust policy denied this execution.")
            }
            resp.needsApproval -> Result(false, "", resp.reason ?: "approval required",
                -1, Path.REFUSED_NEEDS_APPROVAL,
                "Execution requires user approval.")
            else -> Result(resp.success, resp.stdout, resp.stderr,
                resp.exit_code, Path.DAEMON)
        }
    }
}
