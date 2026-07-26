#pragma once

#include <string>

namespace astra::root {

/// Mount API modules use to bind-mount paths inside the Astra namespace.
///
/// Flow:
/// @code
/// AVM Module
///   ↓
/// request mount
///   ↓
/// SecurityEngine
///   ↓
/// Capability Matrix
///   ↓
/// Overlay Mount
/// @endcode
class MountManager {
public:
    bool mount(const std::string& source, const std::string& target);
    bool umount(const std::string& target);
};

}  // namespace astra::root
