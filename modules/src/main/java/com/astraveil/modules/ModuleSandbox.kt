package com.astraveil.modules

import com.astraveil.core.AstraCore
import com.astraveil.core.permission.Permission

/**
 * Computes the sandbox profile for a running module and is the integration
 * point where AstraVeil will eventually enforce that profile at the kernel
 * boundary via the AstraRoot daemon.
 *
 * **This is what distinguishes AstraVeil from Magisk.** In Magisk every
 * module runs as root and can do whatever root can do — there is no
 * isolation between modules, and no isolation between a module and the
 * system. AstraVeil instead gives each .avm module a restricted capability
 * domain: a fixed set of filesystem paths it may touch, an on/off network
 * toggle, and a maximum privilege ceiling it cannot exceed even if the user
 * granted every permission in the manifest.
 *
 * In Phase 0 [enforce] is a stub: it computes the profile and returns `true`
 * without actually confining the process. Real enforcement lands together
 * with the AstraRoot kernel module in a later phase.
 *
 * @param core The AstraVeil core engine; consulted for the global permission
 *             policy and capability snapshot.
 */
class ModuleSandbox(private val core: AstraCore) {

    /**
     * Frozen sandbox profile for a single module.
     *
     * @property moduleId       The module this profile applies to.
     * @property allowedPaths   Filesystem paths the module may read or write.
     *                          Always includes the module's own install path.
     * @property network        `true` if the module may open sockets.
     * @property maxPermission  Ceiling permission — the module may never
     *                          escalate beyond this level, regardless of what
     *                          the user granted at install time.
     */
    data class SandboxProfile(
        val moduleId: String,
        val allowedPaths: List<String>,
        val network: Boolean,
        val maxPermission: Permission
    )

    /**
     * Build a [SandboxProfile] for [module].
     *
     * The profile is derived from the module's manifest and granted
     * permissions:
     *  * [SandboxProfile.allowedPaths] always starts with the module's
     *    install path; if the `"filesystem"` permission was NOT granted the
     *    list contains nothing else.
     *  * [SandboxProfile.network] is `true` iff the `"network"` permission
     *    was granted.
     *  * [SandboxProfile.maxPermission] is the highest permission the module
     *    was granted, capped by the global policy in [core].
     */
    fun profileFor(module: AstraModule): SandboxProfile {
        val paths = buildList {
            add(module.installPath)
            if ("filesystem" in module.grantedPermissions) {
                add("/data/data")
                add("/sdcard")
            }
        }
        val network = "network" in module.grantedPermissions
        val maxPermission = module.grantedPermissions
            .mapNotNull { Permission.fromName(it) }
            .maxByOrNull { it.level }
            ?: Permission.NONE
        return SandboxProfile(
            moduleId = module.id,
            allowedPaths = paths.distinct(),
            network = network,
            maxPermission = maxPermission
        )
    }

    /**
     * Push [profile] to the enforcement layer.
     *
     * **Phase 0 stub.** Returns `true` without doing anything. The real
     * implementation will issue an RPC to the AstraVeil daemon which will, in
     * turn, ask the AstraRoot kernel module to install a seccomp/landlock
     * filter on the calling process.
     *
     * Returns `false` to signal "enforcement not active" once we wire the
     * detection logic in — for now we keep the boolean `true` so callers can
     * treat sandboxing as best-effort without changing control flow.
     */
    fun enforce(profile: SandboxProfile): Boolean {
        // TODO(Phase 2): once the AstraRoot daemon ships, issue:
        //     core.daemonClient.sandboxApply(profile)
        // and return its boolean response. Until then this is a no-op.
        return true
    }
}
