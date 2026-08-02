#pragma once
#include <string>
#include <set>
#include <vector>
#include <unordered_map>
#include <mutex>

namespace astra::security {

/**
 * Capability Inheritance Delegation Tree (Innovation 10).
 *
 * Models hierarchical capability delegation from daemon (root of trust)
 * down to modules and their subprocesses. Inspired by seL4 CNode
 * capability delegation.
 *
 * Invariants:
 * 1. A child can ONLY hold capabilities its parent holds (subset rule).
 * 2. Revoking a parent's capability cascades to ALL descendants.
 * 3. Each level can only delegate a SUBSET of its own capabilities.
 */
struct CapabilityNode {
    std::string id;
    std::string parent_id;
    std::set<std::string> capabilities;
    int depth;
};

class CapabilityTree {
public:
    CapabilityTree() = default;

    void initRoot(const std::set<std::string>& allCapabilities);

    bool delegate(const std::string& parentId,
                  const std::string& childId,
                  const std::set<std::string>& capabilities);

    size_t revoke(const std::string& nodeId, const std::string& capability);

    bool hasCapability(const std::string& nodeId,
                       const std::string& capability) const;

    std::set<std::string> capabilitiesOf(const std::string& nodeId) const;

    void removeSubtree(const std::string& nodeId);

    int depthOf(const std::string& nodeId) const;

    bool validateInvariant() const;

    size_t size() const;

private:
    mutable std::mutex mutex_;
    std::unordered_map<std::string, CapabilityNode> nodes_;
    std::unordered_map<std::string, std::vector<std::string>> children_;

    void revokeRecursive(const std::string& nodeId,
                         const std::string& capability, size_t& count);
    void removeRecursive(const std::string& nodeId);
};

} // namespace astra::security
