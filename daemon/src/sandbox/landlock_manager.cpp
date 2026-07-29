#include "astra/sandbox/landlock_manager.hpp"

#include "astra/logger/logger.hpp"

#ifdef __linux__
#include <linux/landlock.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <fcntl.h>
#endif

#include <cstring>

namespace astra {

LandlockManager::LandlockManager(
    std::vector<std::string> readPaths,
    std::vector<std::string> writePaths
) : readPaths_(std::move(readPaths)),
    writePaths_(std::move(writePaths)) {}

bool LandlockManager::apply() {
#ifdef __linux__
    // ---- 1. Check Landlock availability ----
    const int abi_version = static_cast<int>(
        ::syscall(__NR_landlock_create_ruleset, nullptr, 0,
                  LANDLOCK_CREATE_RULESET_VERSION));
    if (abi_version < 0) {
        ALOGW("LandlockManager: landlock not supported (errno=%d), skipped",
              errno);
        return true;  // not fatal — the rest of the sandbox still runs
    }

    // ---- 2. PR_SET_NO_NEW_PRIVS is required for Landlock to take effect ----
    if (::prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0) {
        ALOGW("LandlockManager: PR_SET_NO_NEW_PRIVS failed (errno=%d)", errno);
        return false;
    }

    // ---- 3. Create the ruleset ----
    struct landlock_ruleset_attr ruleset_attr = {};
    // ABI v1: file access only. ABI v2+ adds directory access.
    // We request both read and write file access types.
    ruleset_attr.handled_access_fs =
        LANDLOCK_ACCESS_FS_EXECUTE |
        LANDLOCK_ACCESS_FS_WRITE_FILE |
        LANDLOCK_ACCESS_FS_READ_FILE |
        LANDLOCK_ACCESS_FS_READ_DIR |
        LANDLOCK_ACCESS_FS_REMOVE_DIR |
        LANDLOCK_ACCESS_FS_REMOVE_FILE |
        LANDLOCK_ACCESS_FS_MAKE_CHAR |
        LANDLOCK_ACCESS_FS_MAKE_DIR |
        LANDLOCK_ACCESS_FS_MAKE_REG |
        LANDLOCK_ACCESS_FS_MAKE_SOCK |
        LANDLOCK_ACCESS_FS_MAKE_FIFO |
        LANDLOCK_ACCESS_FS_MAKE_BLOCK |
        LANDLOCK_ACCESS_FS_MAKE_SYM |
        LANDLOCK_ACCESS_FS_REFER |
        LANDLOCK_ACCESS_FS_TRUNCATE;

    const int ruleset_fd = static_cast<int>(
        ::syscall(__NR_landlock_create_ruleset, &ruleset_attr,
                  sizeof(ruleset_attr), 0));
    if (ruleset_fd < 0) {
        ALOGW("LandlockManager: landlock_create_ruleset failed (errno=%d)", errno);
        return true;  // not fatal
    }

    // ---- 4. Bind read paths ----
    for (const auto& path : readPaths_) {
        if (path.empty()) continue;
        int fd = ::open(path.c_str(), O_PATH | O_CLOEXEC);
        if (fd < 0) {
            ALOGW("LandlockManager: cannot open read path '%s' (errno=%d)",
                  path.c_str(), errno);
            continue;
        }
        struct landlock_path_beneath_attr path_attr = {};
        path_attr.allowed_access =
            LANDLOCK_ACCESS_FS_READ_FILE |
            LANDLOCK_ACCESS_FS_READ_DIR;
        path_attr.parent_fd = static_cast<__u64>(fd);
        if (::syscall(__NR_landlock_add_rule, ruleset_fd,
                      LANDLOCK_RULE_PATH_BENEATH, &path_attr, 0) != 0) {
            ALOGW("LandlockManager: add_rule read '%s' failed (errno=%d)",
                  path.c_str(), errno);
        }
        ::close(fd);
    }

    // ---- 5. Bind write paths ----
    for (const auto& path : writePaths_) {
        if (path.empty()) continue;
        int fd = ::open(path.c_str(), O_PATH | O_CLOEXEC);
        if (fd < 0) {
            ALOGW("LandlockManager: cannot open write path '%s' (errno=%d)",
                  path.c_str(), errno);
            continue;
        }
        struct landlock_path_beneath_attr path_attr = {};
        path_attr.allowed_access =
            LANDLOCK_ACCESS_FS_WRITE_FILE |
            LANDLOCK_ACCESS_FS_REMOVE_FILE |
            LANDLOCK_ACCESS_FS_MAKE_REG |
            LANDLOCK_ACCESS_FS_MAKE_DIR |
            LANDLOCK_ACCESS_FS_REMOVE_DIR |
            LANDLOCK_ACCESS_FS_TRUNCATE;
        path_attr.parent_fd = static_cast<__u64>(fd);
        if (::syscall(__NR_landlock_add_rule, ruleset_fd,
                      LANDLOCK_RULE_PATH_BENEATH, &path_attr, 0) != 0) {
            ALOGW("LandlockManager: add_rule write '%s' failed (errno=%d)",
                  path.c_str(), errno);
        }
        ::close(fd);
    }

    // ---- 6. Enforce the ruleset on the current process ----
    if (::syscall(__NR_landlock_restrict_self, ruleset_fd, 0) != 0) {
        ALOGE("LandlockManager: landlock_restrict_self failed (errno=%d)", errno);
        ::close(ruleset_fd);
        return false;
    }
    ::close(ruleset_fd);

    ALOGI("LandlockManager: enforced (%zu read / %zu write paths, abi=%d)",
          readPaths_.size(), writePaths_.size(), abi_version);
    return true;
#else
    ALOGW("LandlockManager: requires Linux");
    return true;
#endif
}

}  // namespace astra
