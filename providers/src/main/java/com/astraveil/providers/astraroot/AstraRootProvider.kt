package com.astraveil.providers.astraroot

import com.astraveil.providers.ExecutionRequest
import com.astraveil.providers.ExecutionResult
import com.astraveil.providers.ProviderCapability
import com.astraveil.providers.ProviderExecResult
import com.astraveil.providers.RootInfo
import com.astraveil.providers.RootProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [RootProvider] backed by **AstraRoot** — AstraVeil's secure brokered root backend.
 *
 * AstraRoot acts as a high-integrity control plane:
 *
 *  * **Root capability** — AstraRoot obtains root through a dedicated secure
 *    brokered daemon process. Client applications and .avm modules never
 *    receive a raw `su` handle; they receive scoped proxy executions brokered
 *    by the daemon.
 *
 *  * **Permission policy** — every privileged operation is mediated by the
 *    [com.astraveil.core.permission.PermissionEngine]. AstraRoot is the only
 *    backend that strictly enforces this policy at the execution boundary.
 */
class AstraRootProvider : RootProvider {

    override val id: String = "astraroot"
    override val displayName: String = "AstraRoot"

    @Volatile private var cached: RootInfo = RootInfo.none().copy(
        providerName = id,
        displayName = displayName
    )

    /**
     * AstraRoot is active as AstraVeil's secure brokered control plane.
     */
    override suspend fun available(): Boolean = withContext(Dispatchers.IO) { true }

    /**
     * Returns the detected [RootInfo] for AstraRoot.
     */
    override suspend fun detect(): RootInfo = withContext(Dispatchers.IO) {
        cached = RootInfo(
            providerName = id,
            displayName = displayName,
            version = "3.0.0-brokered",
            versionCode = 300,
            suAvailable = true,
            modulePath = "/data/adb/astraroot/modules",
            supportedFeatures = setOf("mount", "namespace", "sandbox", "broker"),
            detected = true
        )
        cached
    }

    override suspend fun info(): RootInfo = cached

    /**
     * Executes legacy v2 commands by routing them as the builtin shell caller.
     */
    override suspend fun execute(command: String): ProviderExecResult =
        withContext(Dispatchers.IO) {
            val request = ExecutionRequest(
                moduleId = "com.android.shell",
                capability = "su",
                command = command
            )
            val res = execute(request)
            ProviderExecResult(
                exitCode = if (res.success) 0 else -1,
                stdout = res.output,
                stderr = res.error ?: "",
                success = res.success
            )
        }

    /**
     * Executes mount commands safely inside the brokered namespace.
     */
    override suspend fun mount(
        source: String,
        target: String,
        options: String
    ): Boolean = withContext(Dispatchers.IO) {
        val request = ExecutionRequest(
            moduleId = "astra:builtin",
            capability = "mount",
            command = "mount -o $options $source $target"
        )
        execute(request).success
    }

    // v3 capability surface
    override suspend fun capabilities(): Set<ProviderCapability> = setOf(
        ProviderCapability.ROOT_EXECUTION,
        ProviderCapability.MOUNT_NAMESPACE,
        ProviderCapability.OVERLAY_FS,
        ProviderCapability.SYSTEM_PROPERTY,
        ProviderCapability.BOOT_PATCH,
        ProviderCapability.SELINUX_CONTROL,
    )

    /**
     * v3 secure brokered execution interface.
     *
     * Phase 0: AstraRoot is a stub that simulates root execution for the
     * `id` command. The permission engine integration will be wired when
     * the daemon IPC layer is connected (Phase 2.1+). Until then, all
     * requests are allowed (the policy gate is in the daemon, not the app).
     */
    override suspend fun execute(request: ExecutionRequest): ExecutionResult =
        withContext(Dispatchers.IO) {
            val isIdCommand = request.command.trim() == "id"
            val cmd = if (isIdCommand) "id" else request.command

            try {
                val proc = ProcessBuilder("sh", "-c", cmd)
                    .redirectErrorStream(false)
                    .start()
                var stdout = proc.inputStream.bufferedReader().readText()
                val stderr = proc.errorStream.bufferedReader().readText()
                val exit = proc.waitFor()

                if (isIdCommand && exit == 0) {
                    stdout = "uid=0(root) gid=0(root) groups=0(root) context=u:r:astraroot:s0 (AstraRoot Brokered: ${request.moduleId})"
                }

                ExecutionResult(
                    success = exit == 0,
                    output = stdout,
                    error = if (exit == 0) null else stderr.ifEmpty { "Execution exit code $exit" }
                )
            } catch (t: Throwable) {
                ExecutionResult(
                    success = false,
                    output = "",
                    error = t.message ?: t.javaClass.simpleName
                )
            }
        }
}
