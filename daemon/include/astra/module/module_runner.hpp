#pragma once

#include <string>

namespace astra::module {

/// v3 module runner — launches each AVM module in its own isolated
/// process instead of loading it into the daemon.
///
/// Architecture:
/// @code
/// AstraDaemon
///     ↓
/// ModuleRunnerManager
///     ↓
/// ProcessBuilder
///     ↓
/// module_runner (isolated process)
///     ↓
/// .avm runtime
/// @endcode
///
/// Phase-2 skeleton: [start]/[stop] are stubs that record intent. Real
/// process isolation (fork + setns + drop caps + dlopen) lands in
/// Phase 4 alongside the AVM runtime.
class ModuleRunner {
public:
    /// Launch the module with @p moduleId in a new isolated process.
    /// Returns true once the process has been spawned.
    bool start(const std::string& moduleId);

    /// Terminate the module with @p moduleId.
    bool stop(const std::string& moduleId);
};

}  // namespace astra::module
