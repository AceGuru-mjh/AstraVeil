#include "astra/capability/capability_matrix.hpp"

namespace astra::capability {

void CapabilityMatrix::set(
    Capability capability,
    bool enabled
) {
    matrix_[capability] = enabled;
}

bool CapabilityMatrix::has(
    Capability capability
) const {
    auto it = matrix_.find(capability);
    if (it == matrix_.end()) {
        return false;
    }
    return it->second;
}

std::string CapabilityMatrix::json() const {
    std::string result = "{";

    bool first = true;
    for (const auto c : all_capabilities()) {
        if (!first) {
            result += ",";
        }
        first = false;
        result += "\"";
        result += capability_name(c);
        result += "\":";
        result += has(c) ? "true" : "false";
    }

    result += "}";
    return result;
}

}  // namespace astra::capability
