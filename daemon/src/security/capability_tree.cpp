#include "astra/security/capability_tree.hpp"
#include "astra/logger/logger.hpp"

#include <algorithm>

namespace astra::security {

void CapabilityTree::initRoot(const std::set<std::string>& allCapabilities) {
    std::lock_guard lock(mutex_);
    nodes_.clear();
    children_.clear();

    CapabilityNode root;
    root.id = "daemon";
    root.parent_id = "";
    root.capabilities = allCapabilities;
    root.depth = 0;

    nodes_["daemon"] = std::move(root);
    children_["daemon"] = {};

    ALOGI("CapabilityTree: root initialized with %zu capabilities",
          allCapabilities.size());
}

bool CapabilityTree::delegate(const std::string& parentId,
                               const std::string& childId,
                               const std::set<std::string>& capabilities) {
    std::lock_guard lock(mutex_);

    auto parentIt = nodes_.find(parentId);
    if (parentIt == nodes_.end()) {
        ALOGE("CapabilityTree: parent '%s' not found", parentId.c_str());
        return false;
    }

    if (nodes_.count(childId) > 0) {
        ALOGE("CapabilityTree: child '%s' already exists", childId.c_str());
        return false;
    }

    // Subset invariant: child cannot have capabilities parent doesn't have.
    for (const auto& cap : capabilities) {
        if (parentIt->second.capabilities.count(cap) == 0) {
            ALOGE("CapabilityTree: '%s' does not hold '%s' — cannot delegate",
                  parentId.c_str(), cap.c_str());
            return false;
        }
    }

    CapabilityNode child;
    child.id = childId;
    child.parent_id = parentId;
    child.capabilities = capabilities;
    child.depth = parentIt->second.depth + 1;

    nodes_[childId] = std::move(child);
    children_[parentId].push_back(childId);
    children_[childId] = {};

    ALOGI("CapabilityTree: delegated %zu caps from '%s' to '%s' (depth=%d)",
          capabilities.size(), parentId.c_str(), childId.c_str(),
          nodes_[childId].depth);
    return true;
}

size_t CapabilityTree::revoke(const std::string& nodeId,
                               const std::string& capability) {
    std::lock_guard lock(mutex_);
    size_t count = 0;
    revokeRecursive(nodeId, capability, count);
    if (count > 0) {
        ALOGI("CapabilityTree: revoked '%s' from %zu nodes (cascade)",
              capability.c_str(), count);
    }
    return count;
}

bool CapabilityTree::hasCapability(const std::string& nodeId,
                                    const std::string& capability) const {
    std::lock_guard lock(mutex_);
    auto it = nodes_.find(nodeId);
    if (it == nodes_.end()) return false;
    return it->second.capabilities.count(capability) > 0;
}

std::set<std::string> CapabilityTree::capabilitiesOf(
    const std::string& nodeId) const {
    std::lock_guard lock(mutex_);
    auto it = nodes_.find(nodeId);
    if (it == nodes_.end()) return {};
    return it->second.capabilities;
}

void CapabilityTree::removeSubtree(const std::string& nodeId) {
    std::lock_guard lock(mutex_);
    removeRecursive(nodeId);
    ALOGI("CapabilityTree: removed subtree at '%s'", nodeId.c_str());
}

int CapabilityTree::depthOf(const std::string& nodeId) const {
    std::lock_guard lock(mutex_);
    auto it = nodes_.find(nodeId);
    return (it != nodes_.end()) ? it->second.depth : -1;
}

bool CapabilityTree::validateInvariant() const {
    std::lock_guard lock(mutex_);
    for (const auto& [id, node] : nodes_) {
        if (node.parent_id.empty()) continue;
        auto parentIt = nodes_.find(node.parent_id);
        if (parentIt == nodes_.end()) return false;
        for (const auto& cap : node.capabilities) {
            if (parentIt->second.capabilities.count(cap) == 0) {
                ALOGE("CapabilityTree: invariant violation — '%s' holds '%s' "
                      "but parent '%s' does not",
                      id.c_str(), cap.c_str(), node.parent_id.c_str());
                return false;
            }
        }
    }
    return true;
}

size_t CapabilityTree::size() const {
    std::lock_guard lock(mutex_);
    return nodes_.size();
}

void CapabilityTree::revokeRecursive(const std::string& nodeId,
                                      const std::string& capability,
                                      size_t& count) {
    auto it = nodes_.find(nodeId);
    if (it == nodes_.end()) return;

    if (it->second.capabilities.erase(capability) > 0) {
        ++count;
    }

    auto childIt = children_.find(nodeId);
    if (childIt != children_.end()) {
        for (const auto& childId : childIt->second) {
            revokeRecursive(childId, capability, count);
        }
    }
}

void CapabilityTree::removeRecursive(const std::string& nodeId) {
    auto childIt = children_.find(nodeId);
    if (childIt != children_.end()) {
        auto childIds = childIt->second;
        for (const auto& childId : childIds) {
            removeRecursive(childId);
        }
        children_.erase(childIt);
    }

    auto nodeIt = nodes_.find(nodeId);
    if (nodeIt != nodes_.end() && !nodeIt->second.parent_id.empty()) {
        auto parentIt = children_.find(nodeIt->second.parent_id);
        if (parentIt != children_.end()) {
            auto& siblings = parentIt->second;
            siblings.erase(
                std::remove(siblings.begin(), siblings.end(), nodeId),
                siblings.end());
        }
    }

    nodes_.erase(nodeId);
}

} // namespace astra::security
