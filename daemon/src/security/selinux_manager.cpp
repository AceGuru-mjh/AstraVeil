#include "astra/security/selinux_manager.hpp"

#include "astra/logger/logger.hpp"

#include <fstream>

namespace astra::security {

bool SELinuxManager::enabled() {
    std::ifstream f("/sys/fs/selinux/enforce");
    return f.good();
}

bool SELinuxManager::enforcing() {
    std::ifstream f("/sys/fs/selinux/enforce");
    int value = -1;
    f >> value;
    return value == 1;
}

bool SELinuxManager::loadPolicy() {
    /*
     * 检测
     *   ↓
     * 加载最小策略
     *   ↓
     * 允许 Astra 必要能力
     *
     * Phase 8.5: stub. The real policy fragment (selinux/astrad.te)
     * is compiled and loaded in Phase 9.
     */
    if (!enabled()) {
        ALOGW("SELinuxManager: SELinux not present, skip policy load");
        return false;
    }
    ALOGI("SELinuxManager: policy load stub (enforcing=%d)",
          enforcing() ? 1 : 0);
    return true;
}

}  // namespace astra::security
