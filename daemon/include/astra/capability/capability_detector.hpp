#pragma once

#include "astra/capability/capability_matrix.hpp"
#include "astra/provider/root_provider.hpp"

namespace astra::capability {

/// Builds a [CapabilityMatrix] for the current device.
///
/// The matrix merges two sources:
///  1. The active [provider::RootProvider]'s reported [capabilities] —
///     plus ROOT_ACCESS whenever the provider is available.
///  2. Independent device-side probes that do not depend on a root
///     backend (SELinux enforce node, mount-namespace availability).
///
/// Call [detect] whenever the active provider changes or after a device
/// state change (e.g. SELinux mode flipped) to refresh the matrix.
class CapabilityDetector {
public:
    /// @param provider  The active root provider, or nullptr when none
    ///                   is active. Null is safe — only device-side
    ///                   probes contribute to the matrix in that case.
    CapabilityMatrix detect(
        provider::RootProvider* provider
    );
};

}  // namespace astra::capability
