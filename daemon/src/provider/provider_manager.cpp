#include "astra/provider/provider_manager.hpp"

#include "astra/provider/no_root_provider.hpp"
#include "astra/provider/magisk_provider.hpp"
#include "astra/provider/kernelsu_provider.hpp"
#include "astra/provider/apatch_provider.hpp"
#include "astra/provider/astra_root_provider.hpp"

#include "astra/logger/logger.hpp"

namespace astra::provider {

ProviderManager::ProviderManager() = default;

void ProviderManager::initialize() {
    /*
     * 检测顺序 (Phase 8):
     *
     * AstraRoot   ← AstraVeil 自研后端, 最高优先级
     *   ↓
     * KernelSU
     *   ↓
     * APatch
     *   ↓
     * Magisk
     *   ↓
     * NoRoot
     *
     * 如果 AstraRoot 自己可用, 应作为第一后端 — 不再依赖外部 root 实现.
     */

    auto astra_root = std::make_unique<AstraRootProvider>();
    if (astra_root->initialize() && astra_root->available()) {
        provider_ = std::move(astra_root);
        ALOGI("ProviderManager: AstraRoot active");
        return;
    }

    auto ksu = std::make_unique<KernelSUProvider>();
    if (ksu->available()) {
        provider_ = std::move(ksu);
        ALOGI("ProviderManager: KernelSU active");
        return;
    }

    auto apatch = std::make_unique<APatchProvider>();
    if (apatch->available()) {
        provider_ = std::move(apatch);
        ALOGI("ProviderManager: APatch active");
        return;
    }

    auto magisk = std::make_unique<MagiskProvider>();
    if (magisk->available()) {
        provider_ = std::move(magisk);
        ALOGI("ProviderManager: Magisk active");
        return;
    }

    provider_ = std::make_unique<NoRootProvider>();
    ALOGI("ProviderManager: NoRoot active");
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
