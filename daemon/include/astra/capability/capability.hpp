#pragma once

#include <string>
#include <vector>

namespace astra::capability {

/// Granular root capabilities a [provider::RootProvider] can offer.
///
/// This is the canonical capability vocabulary used by the daemon's
/// capability matrix (Phase 5) and the future module permission flow
/// (Phase 6/7). Each provider reports the subset it actually supports;
/// the matrix merges them and the permission engine gates modules
/// against the result.
enum class Capability {
    ROOT_ACCESS,        ///< can elevate to uid 0
    SYSTEM_WRITE,       ///< can write /system (via overlay/bind)
    MOUNT_NAMESPACE,    ///< can create mount namespaces
    KERNEL_INTERFACE,   ///< direct kernel hook / syscall surface
    BOOT_PATCH,         ///< can patch boot/vendor_boot/init_boot
    SELINUX_CONTROL,    ///< can set SELinux mode / policies
    NETWORK,            ///< provider-managed network namespace
    IPC_ACCESS,         ///< provider-brokered IPC to modules
};

/// Human-readable name of a capability, e.g. "ROOT_ACCESS".
/// Stable across releases — used as the JSON key in IPC responses
/// and in module manifests.
std::string capability_name(Capability c);

/// Every capability in the enum, in declaration order. Used by the
/// capability matrix to iterate and emit a full report regardless of
/// which provider is active.
std::vector<Capability> all_capabilities();

}  // namespace astra::capability
