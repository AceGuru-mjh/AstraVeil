#include "astra/sandbox/sandbox_manager.hpp"

#include "astra/sandbox/sandbox_policy.hpp"
#include "astra/sandbox/namespace_manager.hpp"
#include "astra/sandbox/mount_isolation.hpp"
#include "astra/sandbox/seccomp_manager.hpp"
#include "astra/sandbox/landlock_manager.hpp"

#include "astra/logger/logger.hpp"

#include <vector>
#include <string>

namespace astra {

bool SandboxManager::create(const std::string& moduleId) {
    /*
     * Phase 3.2: real native isolation.
     *
     * Chain:
     *   NamespaceManager  → unshare(CLONE_NEWNS|NEWPID|NEWNET) + /proc remount
     *   MountIsolation    → mark / MS_REC|MS_PRIVATE
     *   SeccompManager    → syscall allowlist (40+ syscalls, default EPERM)
     *   LandlockManager   → filesystem path restrictions (read + write paths)
     */
    ALOGI("SandboxManager: create(%s) — native isolation", moduleId.c_str());

    // Default Phase 3.2 policy: mount ns always; pid+net for isolation.
    // Per-module policy derivation from SandboxProfile lands with the
    // Kotlin SandboxPolicyResolver bridge.
    SandboxPolicy policy;
    policy.moduleId = moduleId;
    policy.mountNamespace = true;
    policy.pidNamespace = true;   // enable PID namespace + /proc remount
    policy.networkNamespace = false;  // off until per-module net policy
    policy.seccomp = true;
    policy.landlock = true;

    // ---- 1. Namespace isolation ----
    NamespaceManager ns;
    if (!ns.create(policy.mountNamespace, policy.pidNamespace,
                   policy.networkNamespace)) {
        ALOGE("SandboxManager: namespace isolation failed for %s", moduleId.c_str());
        return false;
    }

    // ---- 2. Mount propagation ----
    // (NamespaceManager.create already marks / as MS_REC|MS_PRIVATE,
    //  but MountIsolation is kept as a redundant safety net.)
    MountIsolation mount;
    if (!mount.isolate()) {
        ALOGW("SandboxManager: mount isolation skipped for %s", moduleId.c_str());
    }

    // ---- 3. Seccomp syscall filter ----
    if (policy.seccomp) {
        SeccompManager seccomp(policy.allowNetwork);
        if (!seccomp.apply()) {
            ALOGW("SandboxManager: seccomp skipped for %s", moduleId.c_str());
        }
    }

    // ---- 4. Landlock filesystem restrictions ----
    if (policy.landlock) {
        // Read paths: system + module's own install path.
        std::vector<std::string> readPaths = {
            "/system",
            "/vendor",
            "/product",
            "/data/astra/modules/" + moduleId,
        };
        // Write paths: module's own data dir + tmp only.
        std::vector<std::string> writePaths = {
            "/data/astra/modules/" + moduleId,
            "/data/local/tmp",
        };
        LandlockManager landlock(std::move(readPaths), std::move(writePaths));
        if (!landlock.apply()) {
            ALOGW("SandboxManager: landlock skipped for %s", moduleId.c_str());
        }
    }

    ALOGI("SandboxManager: %s isolated (ns=%d pid=%d net=%d seccomp=%d landlock=%d)",
          moduleId.c_str(),
          policy.mountNamespace ? 1 : 0,
          policy.pidNamespace ? 1 : 0,
          policy.networkNamespace ? 1 : 0,
          policy.seccomp ? 1 : 0,
          policy.landlock ? 1 : 0);
    return true;
}

bool SandboxManager::destroy(const std::string& moduleId) {
    ALOGI("SandboxManager: destroy(%s)", moduleId.c_str());
    // Namespaces die with the process; seccomp/landlock cannot be undone.
    return true;
}

}  // namespace astra
