#pragma once

#include "astra/root/command_runtime.hpp"
#include "astra/root/mount_manager.hpp"
#include "astra/root/file_manager.hpp"
#include "astra/root/hook_manager.hpp"

#include <string>

namespace astra::root {

/// Unified entry point into the AstraRoot runtime.
///
/// Usage:
/// @code
/// AstraRoot root;
/// root.initialize();
/// auto r = root.execute("getprop");
/// @endcode
///
/// Exposes the [CommandRuntime], [MountManager], [FileManager] and
/// [HookManager] through a single facade so AVM modules and the IPC
/// layer never need to know which subsystem a call belongs to.
class AstraRoot {
public:
    bool initialize();

    CommandResult execute(const std::string& command);

    bool mount(std::string source, std::string target);
    bool umount(std::string target);

    bool readFile(const std::string& path, std::string& out);
    bool writeFile(const std::string& path, const std::string& data);

private:
    CommandRuntime command_;
    MountManager   mount_;
    FileManager    file_;
    HookManager    hook_;
    bool ready_ = false;
};

}  // namespace astra::root
