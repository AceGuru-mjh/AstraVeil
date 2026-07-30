#pragma once

namespace astra {

/// Applies a seccomp allowlist filter to the calling process.
///
/// Default policy: deny all syscalls, then allow a baseline set
/// (file I/O, memory, threads, signals, exit — ~40 syscalls). Network
/// syscalls are gated by the [allowNetwork] flag, set from the module's
/// SandboxProfile.
///
/// NOTE: requires libseccomp. When libseccomp is not available the
/// [apply] call logs a warning and returns true so the rest of the
/// sandbox chain still runs; hard enforcement lands when the CMake
/// build links -lseccomp.
class SeccompManager {
public:
    SeccompManager() = default;

    /// @param allowNetwork  if true, socket/connect/bind/sendto/recvfrom
    ///                      etc. are added to the allowlist. Modules with
    ///                      the "network" permission set this to true.
    explicit SeccompManager(bool allowNetwork) : allowNetwork_(allowNetwork) {}

    bool apply();

private:
    bool allowNetwork_ = false;
};

}  // namespace astra
