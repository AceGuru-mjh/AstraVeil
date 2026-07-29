#include "astra/sandbox/seccomp_manager.hpp"

#include "astra/logger/logger.hpp"

#ifdef ASTRA_HAVE_LIBSECCOMP
#include <seccomp.h>
#endif

namespace astra {

/// Convenience macro to keep the allowlist readable.
#ifdef ASTRA_HAVE_LIBSECCOMP
#define ASTRA_ALLOW(ctx, sys)                                        \
    do {                                                            \
        if (seccomp_rule_add((ctx), SCMP_ACT_ALLOW,                \
                             SCMP_SYS(sys), 0) < 0) {              \
            ALOGW("SeccompManager: failed to allow " #sys);       \
        }                                                           \
    } while (0)
#else
#define ASTRA_ALLOW(ctx, sys) ((void)0)
#endif

bool SeccompManager::apply() {
#ifdef ASTRA_HAVE_LIBSECCOMP
    // Default-deny: any syscall not in the allowlist returns EPERM (errno=1).
    // We use SCMP_ACT_ERRNO(1) rather than SCMP_ACT_KILL_PROCESS so that
    // a missing rule produces a graceful failure instead of a SIGKILL —
    // easier to debug during policy iteration.
    scmp_filter_ctx ctx = seccomp_init(SCMP_ACT_ERRNO(1));
    if (!ctx) {
        ALOGE("SeccompManager: seccomp_init failed");
        return false;
    }

    // ---- File I/O ----
    ASTRA_ALLOW(ctx, read);
    ASTRA_ALLOW(ctx, write);
    ASTRA_ALLOW(ctx, open);
    ASTRA_ALLOW(ctx, openat);
    ASTRA_ALLOW(ctx, close);
    ASTRA_ALLOW(ctx, fstat);
    ASTRA_ALLOW(ctx, newfstatat);
    ASTRA_ALLOW(ctx, lstat);
    ASTRA_ALLOW(ctx, lseek);
    ASTRA_ALLOW(ctx, readlink);
    ASTRA_ALLOW(ctx, readlinkat);
    ASTRA_ALLOW(ctx, access);
    ASTRA_ALLOW(ctx, faccessat);
    ASTRA_ALLOW(ctx, getdents);
    ASTRA_ALLOW(ctx, getdents64);

    // ---- Memory management ----
    ASTRA_ALLOW(ctx, mmap);
    ASTRA_ALLOW(ctx, mmap2);
    ASTRA_ALLOW(ctx, munmap);
    ASTRA_ALLOW(ctx, mprotect);
    ASTRA_ALLOW(ctx, brk);
    ASTRA_ALLOW(ctx, madvise);

    // ---- Threading / synchronisation ----
    ASTRA_ALLOW(ctx, futex);
    ASTRA_ALLOW(ctx, set_tid_address);
    ASTRA_ALLOW(ctx, gettid);

    // ---- Process info ----
    ASTRA_ALLOW(ctx, getpid);
    ASTRA_ALLOW(ctx, getppid);
    ASTRA_ALLOW(ctx, getuid);
    ASTRA_ALLOW(ctx, geteuid);
    ASTRA_ALLOW(ctx, getgid);
    ASTRA_ALLOW(ctx, getegid);
    ASTRA_ALLOW(ctx, getgroups);

    // ---- Time ----
    ASTRA_ALLOW(ctx, clock_gettime);
    ASTRA_ALLOW(ctx, clock_gettime64);
    ASTRA_ALLOW(ctx, nanosleep);
    ASTRA_ALLOW(ctx, time);

    // ---- Signals ----
    ASTRA_ALLOW(ctx, rt_sigaction);
    ASTRA_ALLOW(ctx, rt_sigprocmask);
    ASTRA_ALLOW(ctx, rt_sigreturn);
    ASTRA_ALLOW(ctx, sigaltstack);

    // ---- I/O multiplexing ----
    ASTRA_ALLOW(ctx, poll);
    ASTRA_ALLOW(ctx, ppoll);
    ASTRA_ALLOW(ctx, select);
    ASTRA_ALLOW(ctx, pselect6);
    ASTRA_ALLOW(ctx, epoll_create1);
    ASTRA_ALLOW(ctx, epoll_ctl);
    ASTRA_ALLOW(ctx, epoll_wait);
    ASTRA_ALLOW(ctx, epoll_pwait);

    // ---- Misc runtime support ----
    ASTRA_ALLOW(ctx, ioctl);
    ASTRA_ALLOW(ctx, prctl);
    ASTRA_ALLOW(ctx, uname);
    ASTRA_ALLOW(ctx, getrandom);

    // ---- Exit ----
    ASTRA_ALLOW(ctx, exit);
    ASTRA_ALLOW(ctx, exit_group);

    // ---- Architecture-specific TLS access (ARM 32-bit) ----
#ifdef __arm__
    ASTRA_ALLOW(ctx, set_tls);
#endif

    const int rc = seccomp_load(ctx);
    seccomp_release(ctx);
    if (rc != 0) {
        ALOGE("SeccompManager: seccomp_load failed (rc=%d)", rc);
        return false;
    }
    ALOGI("SeccompManager: filter applied (40+ syscalls allowed, default EPERM)");
    return true;
#else
    // libseccomp not linked — log and allow the chain to continue.
    // Hard enforcement lands when the CMake build links -lseccomp.
    ALOGW("SeccompManager: libseccomp not available, filter skipped");
    return true;
#endif
}

}  // namespace astra
