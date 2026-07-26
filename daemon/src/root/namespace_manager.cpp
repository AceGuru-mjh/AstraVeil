#include "astra/root/namespace_manager.hpp"

#ifdef __linux__
#include <sched.h>
#include <unistd.h>
#include <sys/mount.h>
#endif

#include "astra/logger/logger.hpp"

namespace astra::root {

NamespaceManager::NamespaceManager() = default;
NamespaceManager::~NamespaceManager() {
    destroy();
}

bool NamespaceManager::create() {
#ifdef __linux__
    /*
     * 创建独立 mount namespace。
     *
     * unshare(CLONE_NEWNS) moves the calling process into a new mount
     * namespace; subsequent mounts are invisible to other processes.
     */
    if (unshare(CLONE_NEWNS) != 0) {
        ALOGE("NamespaceManager: unshare(CLONE_NEWNS) failed");
        return false;
    }

    /*
     * 阻止 mount 传播: mark / as private so mounts in this namespace
     * never propagate back to the init namespace.
     */
    if (mount(nullptr, "/", nullptr, MS_REC | MS_PRIVATE, nullptr) != 0) {
        ALOGW("NamespaceManager: marking / private failed (non-fatal)");
    }

    namespace_pid_ = getpid();
    ALOGI("NamespaceManager: mount namespace created (pid=%d)",
          static_cast<int>(namespace_pid_));
    return true;
#else
    ALOGW("NamespaceManager: namespace creation requires Linux");
    return false;
#endif
}

bool NamespaceManager::enter() {
    // Phase 8: the namespace is the current process's. setns() lands in
    // Phase 8.5 when the namespace is owned by a long-lived child.
    return namespace_pid_ != -1;
}

bool NamespaceManager::destroy() {
    // Phase 8: nothing to release — the namespace dies with the process.
    namespace_pid_ = -1;
    return true;
}

pid_t NamespaceManager::pid() const {
    return namespace_pid_;
}

}  // namespace astra::root
