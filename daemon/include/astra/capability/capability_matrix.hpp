#pragma once

#include "astra/capability/capability.hpp"

#include <map>
#include <string>

namespace astra::capability {

/// The merged, queryable capability matrix for the running device.
///
/// Built by [CapabilityDetector] from the active [provider::RootProvider]'s
/// reported capabilities plus independent device-side probes (SELinux
/// enforce node, mount-namespace availability, ...). Every subsystem that
/// needs to answer "can the device do X?" reads from this matrix instead
/// of branching on provider identity.
///
/// The permission engine (Phase 6/7) gates module permissions against it:
/// a module requesting BOOT_PATCH on a device where
/// `has(BOOT_PATCH) == false` is denied with "capability unavailable".
class CapabilityMatrix {
public:
    /// Enable / disable a capability in the matrix.
    void set(
        Capability capability,
        bool enabled
    );

    /// @return true iff @p capability is present AND enabled.
    bool has(
        Capability capability
    ) const;

    /// Serialise the full matrix as a JSON object, e.g.
    /// @code
    /// {"ROOT_ACCESS":true,"SELINUX_CONTROL":true,"MOUNT_NAMESPACE":true,
    ///  "BOOT_PATCH":false,...}
    /// @endcode
    /// Every capability in [all_capabilities] is emitted so consumers can
    /// parse the response without prior knowledge of the enum order.
    std::string json() const;

private:
    std::map<Capability, bool> matrix_;
};

}  // namespace astra::capability
