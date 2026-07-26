#pragma once

#include "astra/module/module.hpp"
#include "astra/module/module_permission_checker.hpp"
#include "astra/capability/capability_matrix.hpp"

#include <string>

namespace astra::module {

/// Drives the start/stop lifecycle of an AVM module.
///
/// Start flow:
/// @code
/// 1. read manifest
/// 2. permission check (ModulePermissionChecker vs CapabilityMatrix)
/// 3. create sandbox
/// 4. load module (.so / script)
/// 5. start
/// @endcode
///
/// Phase 6 skeleton: steps 1/3/4 are stubbed (the .avm loader and the
/// Rust-backed sandbox land in later sub-phases). The permission check
/// is live so the deny-path is exercised end-to-end.
class ModuleRuntime {
public:
    ModuleRuntime(
        const capability::CapabilityMatrix& matrix
    );

    /// Start @p module. Returns false if the permission check denies
    /// the module or if a later step fails.
    bool start(
        Module& module
    );

    /// Stop the module with manifest id @p id.
    bool stop(
        std::string id
    );

private:
    ModulePermissionChecker checker_;
    const capability::CapabilityMatrix& matrix_;
};

}  // namespace astra::module
