#include "astra/recovery/crash_guard.hpp"

namespace astra::recovery {

void CrashGuard::registerModule(const std::string& id) {
    crash_counts_[id] = 0;
}

bool CrashGuard::reportCrash(const std::string& id) {
    auto& count = crash_counts_[id];
    ++count;
    return count >= kCrashThreshold;
}

bool CrashGuard::shouldDisable(const std::string& id) {
    auto it = crash_counts_.find(id);
    return it != crash_counts_.end() && it->second >= kCrashThreshold;
}

}  // namespace astra::recovery
