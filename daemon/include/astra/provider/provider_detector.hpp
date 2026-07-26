#pragma once

#include "astra/provider/root_provider.hpp"

#include <memory>

namespace astra::provider {

class ProviderDetector {
public:
    static std::unique_ptr<RootProvider> detect();
};

}  // namespace astra::provider
