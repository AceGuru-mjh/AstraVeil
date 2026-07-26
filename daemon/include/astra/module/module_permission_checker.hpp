#pragma once

#include "astra/module/module.hpp"
#include "astra/capability/capability_matrix.hpp"
#include "astra/capability/capability.hpp"

namespace astra::module {

/// Decides whether a module's requested permissions can be satisfied by
/// the device's current [astra::capability::CapabilityMatrix].
///
/// Permission flow:
/// @code
/// module manifest
///      ↓
/// PermissionEngine
///      ↓
/// CapabilityMatrix       (this checker)
///      ↓
/// RootProvider
///      ↓
/// ALLOW / DENY
/// @endcode
///
/// Each [ModulePermission::name] is resolved to a
/// [astra::capability::Capability] by name. If the name does not map to
/// a known capability, or if the matrix reports the capability as
/// unavailable, the module is denied.
class ModulePermissionChecker {
public:
    /// @return true iff every permission in @p manifest is backed by an
    ///         enabled capability in @p matrix.
    bool check(
        const ModuleManifest& manifest,
        const capability::CapabilityMatrix& matrix
    );
};

}  // namespace astra::module
