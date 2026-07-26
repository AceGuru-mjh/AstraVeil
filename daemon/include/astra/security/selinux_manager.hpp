#pragma once

namespace astra::security {

/// SELinux integration layer.
///
/// AstraRoot does NOT disable SELinux. It detects the current state,
/// loads the minimal Astra policy that grants the daemon the
/// capabilities it needs (sys_admin, dac_override, ...), and leaves
/// enforcement on. This is the difference between "root = bypass
/// SELinux" (Magisk model) and "root = a policy domain with the
/// capabilities it actually needs" (AstraRoot model).
class SELinuxManager {
public:
    /// True iff the SELinux filesystem is mounted (/sys/fs/selinux).
    bool enabled();

    /// True iff SELinux is in enforcing mode.
    bool enforcing();

    /// Load the Astra policy fragment. Phase 8.5 stub — real policy
    /// load lands in Phase 9 alongside the .te files.
    bool loadPolicy();
};

}  // namespace astra::security
