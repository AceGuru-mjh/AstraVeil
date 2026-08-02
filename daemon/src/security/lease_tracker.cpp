#include "astra/security/lease_tracker.hpp"
#include "astra/logger/logger.hpp"

#include <algorithm>
#include <chrono>

namespace astra::security {

int64_t LeaseTracker::nowMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
}

void LeaseTracker::upsert(const std::string& leaseId,
                           const std::string& moduleId,
                           const std::string& capability,
                           int64_t expiresAtMs,
                           bool revoked) {
    std::lock_guard lock(mutex_);
    leases_[leaseId] = LeaseEntry{leaseId, moduleId, capability, expiresAtMs, revoked};
    ALOGI("LeaseTracker: upsert %s (%s/%s, expires=%lld, revoked=%d)",
          leaseId.c_str(), moduleId.c_str(), capability.c_str(),
          static_cast<long long>(expiresAtMs), revoked ? 1 : 0);
}

void LeaseTracker::remove(const std::string& leaseId) {
    std::lock_guard lock(mutex_);
    leases_.erase(leaseId);
}

bool LeaseTracker::hasActiveLease(const std::string& moduleId,
                                   const std::string& capability) const {
    std::lock_guard lock(mutex_);
    const int64_t now = nowMs();
    for (const auto& [id, entry] : leases_) {
        if (entry.module_id == moduleId &&
            entry.capability == capability &&
            !entry.revoked &&
            (entry.expires_at_ms == INT64_MAX || now < entry.expires_at_ms)) {
            return true;
        }
    }
    return false;
}

size_t LeaseTracker::activeCount() const {
    std::lock_guard lock(mutex_);
    const int64_t now = nowMs();
    size_t count = 0;
    for (const auto& [id, entry] : leases_) {
        if (!entry.revoked &&
            (entry.expires_at_ms == INT64_MAX || now < entry.expires_at_ms)) {
            ++count;
        }
    }
    return count;
}

void LeaseTracker::sweep() {
    std::lock_guard lock(mutex_);
    const int64_t now = nowMs();
    size_t swept = 0;
    for (auto& [id, entry] : leases_) {
        if (!entry.revoked &&
            entry.expires_at_ms != INT64_MAX &&
            now >= entry.expires_at_ms) {
            entry.revoked = true;
            ++swept;
            ALOGI("LeaseTracker: expired %s (%s/%s)",
                  id.c_str(), entry.module_id.c_str(), entry.capability.c_str());
        }
    }
    if (swept > 0) {
        ALOGI("LeaseTracker: swept %zu expired leases", swept);
    }
}

} // namespace astra::security
