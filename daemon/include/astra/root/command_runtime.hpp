#pragma once

#include <string>

namespace astra::root {

/// Result of running a command through the AstraRoot runtime.
struct CommandResult {
    bool success = false;
    int exit_code = -1;
    std::string output;
};

/// Runs a shell command and captures its output.
///
/// Phase 8.6: plain popen()-based execution. The [NamespaceExecutor]
/// wraps this to run commands inside the Astra namespace.
class CommandRuntime {
public:
    CommandResult execute(const std::string& command);
};

}  // namespace astra::root
