package com.astraveil.sdk

import android.content.Context
import com.astraveil.core.AstraCore
import com.astraveil.core.capability.CapabilityInfo
import com.astraveil.providers.ProviderExecResult
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Process-wide bootstrap for the AstraVeil SDK.
 *
 * The AstraVeil daemon process constructs the [AstraCore] engine and the
 * [ProviderRegistry] at startup and registers them here via [init] before any
 * third-party module is loaded. Third-party modules then construct an
 * [AstraClient] with just a [Context] — they never see the registry or the
 * core directly.
 */
object AstraSdk {
    @Volatile private var coreRef: AstraCore? = null

    /** True once [init] has been called by the daemon process. */
    val isInitialised: Boolean get() = coreRef != null

    /**
     * Bind the SDK to the running AstraVeil engine. Called exactly once by
     * the daemon at process start.
     */
    fun init(core: AstraCore) {
        coreRef = core
    }

    /** Tear down the binding (used by tests and daemon shutdown). */
    fun reset() {
        coreRef = null
    }

    /** Access the bound [AstraCore], or throw if [init] was not called. */
    internal fun core(): AstraCore =
        coreRef ?: error("AstraSdk not initialised; call AstraSdk.init(core) first")

    /**
     * The active [ProviderRegistry]. A singleton, so no registration is
     * needed — the daemon only has to call [init] with the [AstraCore].
     */
    internal fun registry(): ProviderRegistry = ProviderRegistry
}

/**
 * **Stable public facade** of the AstraVeil SDK.
 *
 * Third-party .avm modules interact with AstraVeil exclusively through this
 * class. Every member here is part of the SDK stability contract: signatures
 * may be added in newer API levels, but never removed or semantically changed
 * without bumping [sdkApiLevel] and shipping a migration guide.
 *
 * Usage from a module:
 * ```
 * val astra = AstraClient(context)
 * if (astra.requestPermission("shell")) {
 *     val r = astra.execute("id -u")
 *     // r.stdout == "0\n" if running as root
 * }
 * ```
 *
 * The facade internally reaches the [AstraCore] singleton (registered by the
 * AstraVeil daemon process via [AstraSdk.init]) and the active
 * [ProviderRegistry]. On devices where no root backend is available,
 * [execute] returns a failed [ProviderExecResult] rather than throwing.
 *
 * @param context Any Android [Context]. Stored as the application context so
 *                the client is safe to retain across configuration changes.
 */
class AstraClient(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Human-readable version of the AstraVeil SDK linked into the caller. */
    val version: String = "0.1.0"

    /**
     * The Module API level implemented by this build. See
     * [AstraSdkConstants.MODULE_API_LEVEL] for the matching constant.
     */
    val sdkApiLevel: Int = 1

    /**
     * Return the [CapabilityInfo] snapshot describing what AstraVeil can do on
     * this device (active backend, supported features, sandbox availability).
     */
    suspend fun getCapability(): CapabilityInfo = withContext(Dispatchers.IO) {
        AstraSdk.core().capabilityEngine.scan()
    }

    /**
     * Ask the user (or the cached permission policy) to grant [permission] to
     * the calling module. Returns `true` if the permission is currently held
     * by the caller (whether newly granted or already present).
     *
     * [permission] must be one of [AstraSdkConstants.SUPPORTED_PERMISSIONS];
     * any other token returns `false` without prompting the user.
     */
    suspend fun requestPermission(permission: String): Boolean = withContext(Dispatchers.IO) {
        if (permission !in AstraSdkConstants.SUPPORTED_PERMISSIONS) return@withContext false
        AstraSdk.core().permissionEngine.request(permission)
    }

    /**
     * Execute [command] through the active
     * [com.astraveil.providers.RootProvider], if any. On devices without root
     * the call returns a failed [ProviderExecResult] (`success = false`,
     * `exitCode = -1`).
     *
     * The caller MUST hold the `"shell"` permission, otherwise the call is
     * rejected without dispatching the command.
     */
    suspend fun execute(command: String): ProviderExecResult = withContext(Dispatchers.IO) {
        val core = AstraSdk.core()
        if (!core.permissionEngine.has("shell")) {
            return@withContext ProviderExecResult(
                exitCode = -1,
                stdout = "",
                stderr = "Permission 'shell' not granted",
                success = false
            )
        }
        val registry = AstraSdk.registry()
        val active = registry.detectActive()?.providerName
            ?: return@withContext ProviderExecResult(
                exitCode = -1,
                stdout = "",
                stderr = "No root provider available",
                success = false
            )
        val provider = registry.byId(active)
            ?: return@withContext ProviderExecResult(
                exitCode = -1,
                stdout = "",
                stderr = "Active provider '$active' not registered",
                success = false
            )
        provider.execute(command)
    }
}
