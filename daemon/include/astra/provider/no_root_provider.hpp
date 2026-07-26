#pragma once

#include "astra/provider/root_provider.hpp"

namespace astra::provider {

/// Fallback provider used when no root backend is detected.
/// Reports NONE type, available()=false, execute()=false, no capabilities.
class NoRootProvider final : public RootProvider {
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
