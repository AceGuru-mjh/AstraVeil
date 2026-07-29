#include "astra/sandbox/namespace_manager.hpp"

#ifdef __linux__
#include <sched.h>
#include <sys/mount.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
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
        ALOGE("NamespaceManager: unshare(0x%x) failed (errno=%d)", flags, errno);
        return false;
    }

    // ---- Mount propagation: mark / as MS_REC|MS_PRIVATE ----
    // This prevents mounts in this namespace from propagating back to
    // the init mount tree (and vice versa).
    if (mount(nullptr, "/", nullptr, MS_REC | MS_PRIVATE, nullptr) != 0) {
        ALOGW("NamespaceManager: marking / private failed (errno=%d, non-fatal)", errno);
    }

    // ---- Remount /proc when a PID namespace is active ----
    // In a PID namespace, /proc must be remounted to show only the
    // namespace's PIDs. Without this, the sandboxed process can read
    // /proc/1/root and escape the mount namespace.
    if (pidNs) {
        // Best-effort: mount a fresh procfs over /proc. If /proc is
        // read-only or we lack CAP_SYS_ADMIN, this fails gracefully.
        if (mount("proc", "/proc", "proc", MS_NOSUID | MS_NOEXEC | MS_NODEV, nullptr) != 0) {
            // Try unmounting the old /proc first, then remount.
            if (umount2("/proc", MNT_DETACH) == 0) {
                if (mount("proc", "/proc", "proc", MS_NOSUID | MS_NOEXEC | MS_NODEV, nullptr) != 0) {
                    ALOGW("NamespaceManager: /proc remount failed (errno=%d, non-fatal)", errno);
                } else {
                    ALOGI("NamespaceManager: /proc remounted for PID namespace");
                }
            } else {
                ALOGW("NamespaceManager: /proc detach failed (errno=%d, non-fatal)", errno);
            }
        } else {
            ALOGI("NamespaceManager: /proc mounted for PID namespace");
        }
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
