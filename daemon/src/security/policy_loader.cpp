#include "astra/security/policy_loader.hpp"

#include "astra/logger/logger.hpp"

#include <filesystem>

namespace astra::security {

bool PolicyLoader::load(const std::string& policy_path) {
    if (!std::filesystem::exists(policy_path)) {
        ALOGW("PolicyLoader: %s not found", policy_path.c_str());
        return false;
    }
    // TODO(Phase 9): compile the .te fragment and write it to
    // /sys/fs/selinux/load. Real policy load requires the fragment to
    // be compiled with checkpolicy first.
    ALOGI("PolicyLoader: load %s (stub)", policy_path.c_str());
    return true;
}

}  // namespace astra::security
