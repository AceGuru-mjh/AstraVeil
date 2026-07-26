#pragma once

#include "astra/provider/root_provider.hpp"

namespace astra::provider {

/// APatch root backend. Patch-based root that injects a su binary by
/// patching the kernel image directly (no kernel rebuild required).
/// Detected via /data/adb/ap.
class APatchProvider final : public RootProvider {
public:
    RootType type() const override;
    bool available() const override;
    bool execute(
        const std::string& command,
        std::string& output
    ) override;
    std::string name() const override;
    std::vector<capability::Capability> capabilities() const override;
};

}  // namespace astra::provider
