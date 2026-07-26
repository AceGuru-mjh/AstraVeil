#include "astra/sandbox/landlock_manager.hpp"

#include "astra/logger/logger.hpp"

#ifdef __linux__
#include <linux/landlock.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <unistd.h>
#endif

#include <fcntl.h>

namespace astra {

LandlockManager::LandlockManager(
    std::vector<std::string> readPaths,
    std::vector<std::string> writePaths
) : readPaths_(std::move(readPaths)),
    writePaths_(std::move(writePaths)) {}

bool LandlockManager::apply() {
#ifdef __linux__
    // Landland ABI v1 (Linux 5.13+). We attempt the syscall; if the
    // kernel lacks Landlock the syscall returns -ENOSYS and we no-op.
    const int ruleset_fd = static_cast<int>(
        ::syscall(__NR_landlock_create_ruleset, nullptr, 0,
                  LANDLOCK_CREATE_RULESET_VERSION));
    if (ruleset_fd < 0) {
        ALOGW("LandlockManager: landlock not supported (errno=%d), skipped",
              -ruleset_fd);
        return true;  // not fatal — the rest of the sandbox still runs
    }
    ::close(ruleset_fd);

    // PR_SET_NO_NEW_PRIVS is required for Landlock to take effect.
    if (::prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0) {
        ALOGW("LandlockManager: PR_SET_NO_NEW_PRIVS failed");
        return false;
    }

    // Full ruleset creation + path binding lands in Phase 3.3 once the
    // Landlock struct layout is wired. For now we record that Landlock
    // is available and the process has dropped new-priv ability.
    ALOGI("LandlockManager: landlock available, NO_NEW_PRIVS set (%zu R / %zu W paths)",
          readPaths_.size(), writePaths_.size());
    return true;
#else
    ALOGW("LandlockManager: requires Linux");
    return true;
#endif
}

}  // namespace astra
