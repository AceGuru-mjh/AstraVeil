#include "astra/provider/provider_manager.hpp"

#include "astra/provider/no_root_provider.hpp"
#include "astra/provider/magisk_provider.hpp"
#include "astra/provider/kernelsu_provider.hpp"

namespace astra::provider {

ProviderManager::ProviderManager() = default;

void ProviderManager::initialize() {
    /*
     * 检测顺序:
     *
     * KernelSU
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

}  // namespace astra::provider
