#pragma once

#include "astra/provider/root_provider.hpp"

#include <memory>

namespace astra::provider {

/// Owns the active RootProvider and provides a stable entry point for the
/// rest of the daemon (PermissionService, IPC layer, ...).
///
/// Detection order in [initialize]:
///   KernelSU → Magisk → NoRoot
///
/// The first provider whose [RootProvider::available] returns true wins.
/// Once selected the provider is held for the lifetime of the manager;
/// call [initialize] again to re-probe.
class ProviderManager {
public:
    ProviderManager();

    /// Probe every known backend and select the first available one.
    void initialize();

    /// Borrow the active provider (non-owning). May return nullptr before
    /// [initialize] has been called.
    RootProvider* current();

    /// Convenience: the [RootType] of the active provider.
    RootType type();

private:
    std::unique_ptr<RootProvider> provider_;
};

}  // namespace astra::provider
