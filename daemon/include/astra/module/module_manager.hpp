#pragma once

#include "astra/module/module.hpp"

#include <string>
#include <vector>

namespace astra::module {

/// Owns the lifecycle of every installed AVM module on the device.
///
/// Responsibilities (Phase 6 surface — full implementations land
/// alongside the .avm loader in later sub-phases):
///  - install(path)   scan/validate an .avm package, register it
///  - remove(id)      unregister and delete a module
///  - list()          snapshot of every registered module
///
/// The manager is the single registry the IPC layer (InstallModule /
/// RemoveModule / ListModules requests) and the [ModuleRuntime] read
/// from. Thread-safety is the caller's responsibility in Phase 6.
class ModuleManager {
public:
    /// Register a module at @p path. Returns true on success.
    bool install(
        const std::string& path
    );

    /// Remove the module with manifest id @p id. Returns true if a
    /// module was removed.
    bool remove(
        const std::string& id
    );

    /// Snapshot of every registered module.
    std::vector<Module> list();

private:
    std::vector<Module> modules_;
};

}  // namespace astra::module
