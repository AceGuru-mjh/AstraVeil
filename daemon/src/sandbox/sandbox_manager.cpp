#include "astra/sandbox/sandbox_manager.hpp"

#include "astra/logger/logger.hpp"

namespace astra {

bool SandboxManager::create(const std::string& moduleId) {
    /*
     * Phase 3.1: 建立隔离入口.
     *
     * Phase 4 will add:
     *   unshare(CLONE_NEWNS)   — mount namespace
     *   unshare(CLONE_NEWPID)  — pid namespace
     *   unshare(CLONE_NEWNET)  — network namespace (if profile denies net)
     *   seccomp filter         — syscall allowlist
     *   landlock rules         — filesystem path restrictions
     */
    ALOGI("SandboxManager: create(%s) — Phase 3.1 stub", moduleId.c_str());
    return true;
}

bool SandboxManager::destroy(const std::string& moduleId) {
    ALOGI("SandboxManager: destroy(%s) — Phase 3.1 stub", moduleId.c_str());
    return true;
}

}  // namespace astra
