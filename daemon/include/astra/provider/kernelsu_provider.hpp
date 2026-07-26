#pragma once

#include "astra/provider/root_provider.hpp"

namespace astra::provider {

/// KernelSU root backend. Detected via /data/adb/ksu.
class KernelSUProvider final : public RootProvider {
public:
    RootType type() const override;
    bool available() const override;
    bool execute(
        const std::string& command,
        std::string& output
    ) override;
    std::string name() const override;
};

}  // namespace astra::provider
