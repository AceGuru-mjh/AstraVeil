#include "astra/provider/no_root_provider.hpp"

namespace astra::provider {

RootType NoRootProvider::type() const {
    return RootType::NONE;
}

bool NoRootProvider::available() const {
    return false;
}

bool NoRootProvider::execute(
    const std::string&,
    std::string&
) {
    return false;
}

std::string NoRootProvider::name() const {
    return "none";
}

std::vector<capability::Capability> NoRootProvider::capabilities() const {
    // No root backend -> no capabilities.
    return {};
}

}  // namespace astra::provider
