#include "astra/provider/provider_manager.hpp"

#include "astra/provider/no_root_provider.hpp"
#include "astra/provider/magisk_provider.hpp"
#include "astra/provider/kernelsu_provider.hpp"
#include "astra/provider/apatch_provider.hpp"

namespace astra::provider {

ProviderManager::ProviderManager() = default;

void ProviderManager::initialize() {
    /*
     * 检测顺序:
     *
     * KernelSU
     *   ↓
     * APatch
     *   ↓
     * Magisk
     *   ↓
     * NoRoot
     */

    auto ksu = std::make_unique<KernelSUProvider>();
    if (ksu->available()) {
        provider_ = std::move(ksu);
        return;
    }

    auto apatch = std::make_unique<APatchProvider>();
    if (apatch->available()) {
        provider_ = std::move(apatch);
        return;
    }

    auto magisk = std::make_unique<MagiskProvider>();
    if (magisk->available()) {
        provider_ = std::move(magisk);
        return;
    }

    provider_ = std::make_unique<NoRootProvider>();
}

RootProvider* ProviderManager::current() {
    return provider_.get();
}

RootType ProviderManager::type() {
    if (!provider_) {
        return RootType::NONE;
    }
    return provider_->type();
}

std::vector<capability::Capability> ProviderManager::capabilities() {
    if (!provider_) {
        return {};
    }
    return provider_->capabilities();
}

}  // namespace astra::provider
