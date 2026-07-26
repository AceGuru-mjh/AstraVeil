#pragma once

#include "astra/provider/root_provider.hpp"

namespace astra::provider {

class NoRootProvider final
        : public RootProvider {
public:
    RootType type() const override {
        return RootType::NONE;
    }

    bool available() const override {
        return false;
    }

    bool execute(
        const std::string&,
        std::string&
    ) override {
        return false;
    }

    std::string name() const override {
        return "none";
    }
};

}  // namespace astra::provider
