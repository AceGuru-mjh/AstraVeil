#include "astra/root/namespace_context.hpp"

#ifdef __linux__
#include <sched.h>
#include <unistd.h>
#include <sys/mount.h>
#include <sys/wait.h>
#endif

#include "astra/logger/logger.hpp"

namespace astra::root {

NamespaceContext::NamespaceContext() = default;
NamespaceContext::~NamespaceContext() {
#ifdef __linux__
    if (namespace_pid_ > 0) {
        // Reap the holder child.
        ::kill(namespace_pid_, SIGTERM);
        int status = 0;
        ::waitpid(namespace_pid_, &status, 0);
    }
#endif
}

bool NamespaceContext::create() {
#ifdef __linux__
    pid_t child = fork();
    if (child < 0) {
        ALOGE("NamespaceContext: fork failed");
        return false;
    }
    if (child == 0) {
        /*
         * 创建独立 mount namespace
         */
        if (unshare(CLONE_NEWNS) != 0) {
            ALOGE("NamespaceContext: unshare failed");
            _exit(1);
        }
        // Mark / private so mounts do not propagate to init.
        if (mount(nullptr, "/", nullptr, MS_REC | MS_PRIVATE, nullptr) != 0) {
            ALOGW("NamespaceContext: / private failed (non-fatal)");
        }
        // Hold the namespace open until the parent reaps us.
        while (true) {
            sleep(60);
        }
        _exit(0);
    }
    namespace_pid_ = child;
    ALOGI("NamespaceContext: holder child pid=%d",
          static_cast<int>(namespace_pid_));
    return true;
#else
    return false;
#endif
}

bool NamespaceContext::isolateMount() {
#ifdef __linux__
    if (namespace_pid_ < 0) {
        return false;
    }
    // Mount isolation is applied in the child at create() time. This
    // method is the future hook for runtime re-isolation.
    return true;
#else
    return false;
#endif
}

bool NamespaceContext::switchNamespace() {
#ifdef __linux__
    if (namespace_pid_ < 0) {
        return false;
    }
    // TODO(Phase 8.5): open /proc/<pid>/ns/mnt and setns().
    ALOGI("NamespaceContext: switchNamespace (setns stub) pid=%d",
          static_cast<int>(namespace_pid_));
    return true;
#else
    return false;
#endif
}

pid_t NamespaceContext::pid() const {
    return namespace_pid_;
}

}  // namespace astra::root
