#include "astra/provider/provider_registry.hpp"

#include "astra/provider/provider_manager.hpp"

#include <algorithm>

namespace astra::provider {

ProviderRegistry::ProviderRegistry() = default;
ProviderRegistry::~ProviderRegistry() = default;

void ProviderRegistry::attach(ProviderManager* manager) {
    manager_ = manager;
}

RootProvider* ProviderRegistry::resolve(capability::Capability cap) {
    if (!manager_) {
        return nullptr;
    }
    auto* p = manager_->current();
    if (!p || !p->available()) {
        return nullptr;
    }
    const auto caps = p->capabilities();
    if (std::find(caps.begin(), caps.end(), cap) == caps.end()) {
        return nullptr;
    }
    return p;
}

}  // namespace astra::provider
