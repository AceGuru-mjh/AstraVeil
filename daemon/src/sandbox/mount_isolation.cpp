#include "astra/sandbox/mount_isolation.hpp"

#ifdef __linux__
#include <sys/mount.h>
#endif

#include "astra/logger/logger.hpp"

namespace astra {

bool MountIsolation::isolate() {
#ifdef __linux__
    if (mount(nullptr, "/", nullptr, MS_REC | MS_PRIVATE, nullptr) != 0) {
        ALOGW("MountIsolation: marking / private failed (non-fatal)");
        return false;
    }
    ALOGI("MountIsolation: / marked MS_REC|MS_PRIVATE");
    return true;
#else
    return false;
#endif
}

}  // namespace astra
