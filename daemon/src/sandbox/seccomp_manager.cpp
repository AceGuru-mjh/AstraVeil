#include "astra/sandbox/seccomp_manager.hpp"

#include "astra/logger/logger.hpp"

#ifdef ASTRA_HAVE_LIBSECCOMP
#include <seccomp.h>
#endif

namespace astra {

bool SeccompManager::apply() {
#ifdef ASTRA_HAVE_LIBSECCOMP
    scmp_filter_ctx ctx = seccomp_init(SCMP_ACT_ERRNO(1));
    if (!ctx) {
        ALOGE("SeccompManager: seccomp_init failed");
        return false;
    }
    // Minimal allowlist.
    seccomp_rule_add(ctx, SCMP_ACT_ALLOW, SCMP_SYS(read), 0);
    seccomp_rule_add(ctx, SCMP_ACT_ALLOW, SCMP_SYS(write), 0);
    seccomp_rule_add(ctx, SCMP_ACT_ALLOW, SCMP_SYS(exit), 0);
    seccomp_rule_add(ctx, SCMP_ACT_ALLOW, SCMP_SYS(exit_group), 0);
    const int rc = seccomp_load(ctx);
    seccomp_release(ctx);
    if (rc != 0) {
        ALOGE("SeccompManager: seccomp_load failed");
        return false;
    }
    ALOGI("SeccompManager: filter applied (read/write/exit allowed)");
    return true;
#else
    // libseccomp not linked — log and allow the chain to continue.
    ALOGW("SeccompManager: libseccomp not available, filter skipped");
    return true;
#endif
}

}  // namespace astra
