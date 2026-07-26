#pragma once

#include <string>
#include <vector>

namespace astra {

/// Applies Landlock filesystem-path restrictions to the calling process.
///
/// Restricts the process to the listed [readPaths] + [writePaths]; every
/// other path becomes inaccessible. Requires kernel Landlock support
/// (Linux 5.13+). When Landlock is unavailable [apply] logs a warning
/// and returns true so the rest of the sandbox chain still runs.
class LandlockManager {
public:
    LandlockManager() = default;

    /// @param readPaths  paths the sandboxed module may read.
    /// @param writePaths paths the sandboxed module may write.
    LandlockManager(
        std::vector<std::string> readPaths,
        std::vector<std::string> writePaths
    );

    bool apply();

private:
    std::vector<std::string> readPaths_;
    std::vector<std::string> writePaths_;
};

}  // namespace astra
