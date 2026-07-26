#pragma once

#include "astra/provider/root_provider.hpp"

namespace astra::provider {

/// Magisk root backend. Detected via /sbin/.magisk.
class MagiskProvider final : public RootProvider {
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
