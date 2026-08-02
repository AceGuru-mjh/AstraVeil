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
