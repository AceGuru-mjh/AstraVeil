#include "astra/sandbox/namespace_manager.hpp"

#ifdef __linux__
#include <sched.h>
#endif

#include "astra/logger/logger.hpp"

namespace astra {

bool NamespaceManager::create(bool mountNs, bool pidNs, bool netNs) {
#ifdef __linux__
    int flags = 0;
    if (mountNs) flags |= CLONE_NEWNS;
    if (pidNs)   flags |= CLONE_NEWPID;
    if (netNs)   flags |= CLONE_NEWNET;
    if (flags == 0) {
        return true;  // nothing requested
    }
    if (unshare(flags) != 0) {
        ALOGE("NamespaceManager: unshare(0x%x) failed", flags);
        return false;
    }
    ALOGI("NamespaceManager: unshare ok (mount=%d pid=%d net=%d)",
          mountNs ? 1 : 0, pidNs ? 1 : 0, netNs ? 1 : 0);
    return true;
#else
    ALOGW("NamespaceManager: namespace creation requires Linux");
    return false;
#endif
}

bool NamespaceManager::enter() {
    // Phase 3.2: setns() lands when the namespace is held by a child.
    return true;
}

}  // namespace astra
