#pragma once

// astra/executor/command_executor.hpp
//
// Run shell commands on behalf of modules. Two flavours:
//
// * `execute(cmd)` — runs as the daemon's own uid. Safe and always
//   available. Used for diagnostics and capability probing.
// * `execute_as_root(cmd)` — runs through the active root provider.
//   Requires `DaemonContext::provider_online == true`. Currently a
//   stub; will be wired up per-provider (su -c, ksu, apatch, ...).

#include <string>

namespace astra::executor {

struct ExecResult {
    int exit_code = -1;
    std::string stdout_;
    std::string stderr_;
};

class CommandExecutor {
public:
    CommandExecutor() = default;

    /// Run `cmd` via `popen("cmd 2>&1")` and capture combined output.
    /// The exit code reflects the child's status; -1 indicates a
    /// `popen` failure.
    ExecResult execute(const std::string& cmd) const;

    /// Run `cmd` as root via the active provider. Currently a stub.
    /// TODO(provider-integration): dispatch to magisk `su -c`, ksu, ap.
    ExecResult execute_as_root(const std::string& cmd) const;
};

}  // namespace astra::executor
