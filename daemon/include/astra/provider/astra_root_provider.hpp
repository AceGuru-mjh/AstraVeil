#pragma once

#include "astra/provider/root_provider.hpp"
#include "astra/root/root_runtime.hpp"

#include <memory>

namespace astra::provider {

/// AstraVeil's own root backend.
///
/// Unlike [MagiskProvider] / [KernelSUProvider] / [APatchProvider] which
/// detect an external root installation, [AstraRootProvider] drives the
/// built-in [root::RootRuntime]: it brings up the Astra namespace +
/// overlay layer at [initialize] time and reports the AstraRoot
/// capability surface (ROOT_ACCESS, ASTRA_ROOT, NAMESPACE_ISOLATION,
/// OVERLAYFS, BOOT_PATCH) once [ready].
///
/// This is the backend AstraVeil eventually self-hosts on; the other
/// providers remain as compatibility shims for devices that already run
/// Magisk / KernelSU / APatch.
class AstraRootProvider final : public RootProvider {
public:
    AstraRootProvider();
    ~AstraRootProvider() override;

    /// Bring up the [RootRuntime]. Idempotent. After this returns true
    /// [available] reflects runtime readiness.
    bool initialize();

    RootType type() const override;
    bool available() const override;
    bool execute(
        const std::string& command,
        std::string& output
    ) override;
    std::string name() const override;
    std::vector<capability::Capability> capabilities() const override;

    /// Borrow the underlying runtime (for the daemon's direct use).
    root::RootRuntime* runtime() { return runtime_.get(); }

private:
    std::unique_ptr<root::RootRuntime> runtime_;
    bool initialized_ = false;
};

}  // namespace astra::provider
