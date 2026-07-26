#pragma once

#include "astra/capability/capability.hpp"
#include "astra/provider/root_provider.hpp"

#include <map>
#include <memory>
#include <string>

namespace astra::provider {

class ProviderManager;

/// Capability-indexed provider registry.
///
/// Complements [ProviderManager] (which holds the *active* backend) by
/// providing a capability → provider lookup. When a request arrives
/// tagged with a capability, the registry resolves it to the active
/// provider iff that provider advertises the capability.
class ProviderRegistry {
public:
    ProviderRegistry();
    ~ProviderRegistry();

    /// Wire the registry to the daemon's ProviderManager. The registry
    /// does not own the manager.
    void attach(ProviderManager* manager);

    /// Resolve the capability to the active provider iff it advertises
    /// @p cap. Returns nullptr if no manager is attached, no provider is
    /// active, or the active provider does not offer @p cap.
    RootProvider* resolve(capability::Capability cap);

    /// Borrow the underlying manager (for ExecutionPipeline).
    ProviderManager* manager() const { return manager_; }

private:
    ProviderManager* manager_ = nullptr;
};

}  // namespace astra::provider
