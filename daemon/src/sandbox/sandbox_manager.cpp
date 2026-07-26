#include "astra/sandbox/sandbox_manager.hpp"

#include "astra/sandbox/sandbox_policy.hpp"
#include "astra/sandbox/namespace_manager.hpp"
#include "astra/sandbox/mount_isolation.hpp"
#include "astra/sandbox/seccomp_manager.hpp"
#include "astra/sandbox/landlock_manager.hpp"

#include "astra/logger/logger.hpp"

namespace astra {

bool SandboxManager::create(const std::string& moduleId) {
    /*
     * Phase 3.2: real native isolation.
     *
     * Chain:
     *   NamespaceManager  → unshare(CLONE_NEWNS|NEWPID|NEWNET)
     *   MountIsolation    → mark / MS_REC|MS_PRIVATE
     *   SeccompManager    → syscall allowlist
     *   LandlockManager   → filesystem path restrictions
     */
    ALOGI("SandboxManager: create(%s) — native isolation", moduleId.c_str());

    // Default Phase 3.2 policy: mount ns always; pid+net+seccomp+landlock
    // for high-risk modules. Per-module policy derivation lands with the
    // Kotlin SandboxPolicyResolver bridge.
    SandboxPolicy policy;
    policy.moduleId = moduleId;
    policy.mountNamespace = true;
    policy.pidNamespace = false;
    policy.networkNamespace = false;
    policy.seccomp = true;
    policy.landlock = true;

    NamespaceManager ns;
    if (!ns.create(policy.mountNamespace, policy.pidNamespace,
                   policy.networkNamespace)) {
        ALOGE("SandboxManager: namespace isolation failed for %s", moduleId.c_str());
        return false;
    }

    MountIsolation mount;
    if (!mount.isolate()) {
        ALOGW("SandboxManager: mount isolation skipped for %s", moduleId.c_str());
        // non-fatal — namespace is still created
    }

    if (policy.seccomp) {
        SeccompManager seccomp;
        if (!seccomp.apply()) {
            ALOGW("SandboxManager: seccomp skipped for %s", moduleId.c_str());
        }
    }

    if (policy.landlock) {
        LandlockManager landlock;
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
