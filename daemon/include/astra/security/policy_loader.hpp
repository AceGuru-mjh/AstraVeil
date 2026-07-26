#pragma once

#include <string>

namespace astra::security {

/// Loads a SELinux policy fragment into the running kernel.
///
/// SELinuxManager
///     ↓
/// PolicyLoader
///     ↓
/// Astra Policy
///     ↓
/// Android SELinux
class PolicyLoader {
public:
    bool load(const std::string& policy_path);
};

}  // namespace astra::security
