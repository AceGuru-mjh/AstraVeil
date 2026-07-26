#pragma once

#include "astra/capability/capability.hpp"
#include "astra/provider/root_provider.hpp"

#include <memory>
#include <vector>

namespace astra::provider {

/// Owns the active RootProvider and provides a stable entry point for the
/// rest of the daemon (PermissionService, IPC layer, ...).
///
/// Detection order in [initialize]:
///   KernelSU → APatch → Magisk → NoRoot
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

    /// The capability set offered by the active provider. Returns an
    /// empty vector when no provider is active (NoRoot reports none).
    /// This is the single source of truth the capability matrix (Phase 5)
    /// and the permission engine read from.
    std::vector<capability::Capability> capabilities();

private:
    std::unique_ptr<RootProvider> provider_;
};

}  // namespace astra::provider
