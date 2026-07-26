#pragma once

namespace astra {

/// Applies a seccomp allowlist filter to the calling process.
///
/// Default policy: deny all syscalls, then allow a minimal read/write/
/// exit set. Real production policy is generated from the module's
/// [SandboxProfile] — this is the Phase 3.2 baseline.
///
/// NOTE: requires libseccomp. When libseccomp is not available the
/// [apply] call logs a warning and returns true so the rest of the
/// sandbox chain still runs; hard enforcement lands when the CMake
/// build links -lseccomp.
class SeccompManager {
public:
    bool apply();
};

}  // namespace astra
