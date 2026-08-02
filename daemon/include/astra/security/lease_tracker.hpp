#pragma once
#include <string>
#include <unordered_map>
#include <mutex>
#include <cstdint>

namespace astra::security {

/**
 * Daemon-side lease tracker (Innovation 1, daemon enforcement point).
 *
 * The Kotlin LeaseManager is the AUTHORITY (issues/revokes leases).
 * This tracker is the ENFORCER: PolicyBridge consults it before
 * allowing execution. If a lease has expired, execution is denied
 * even if the Rust policy would allow it.
 *
 * The app notifies the daemon of lease changes via IPC
 * (RequestType::UpdateLease).
 */
struct LeaseEntry {
    std::string lease_id;
    std::string module_id;
    std::string capability;
    int64_t expires_at_ms;
    bool revoked;
};

class LeaseTracker {
public:
    void upsert(const std::string& leaseId,
                const std::string& moduleId,
                const std::string& capability,
                int64_t expiresAtMs,
                bool revoked);

    void remove(const std::string& leaseId);

    bool hasActiveLease(const std::string& moduleId,
                        const std::string& capability) const;

    size_t activeCount() const;

    void sweep();

private:
    mutable std::mutex mutex_;
    std::unordered_map<std::string, LeaseEntry> leases_;
    static int64_t nowMs();
};

} // namespace astra::security
```

**文件：`daemon/src/security/lease_tracker.cpp`**

```cpp
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
```

