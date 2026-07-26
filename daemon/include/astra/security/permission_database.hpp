#pragma once

#include "astra/security/permission.hpp"

#include <map>
#include <set>
#include <string>

namespace astra::security {

/// Persistent store of user-granted permissions, keyed by module id.
///
/// This is AstraVeil's analogue of Android's runtime permission database:
/// a module may declare that it *wants* ROOT_ACCESS, but the module only
/// actually receives it after the user grants it via the AstraUI Permission
/// Center. Grants survive daemon restarts (the on-disk format is JSON at
/// `/data/astra/permissions.json`).
///
/// Phase 7 in-memory implementation — persistence to disk lands in a
/// later sub-phase alongside the AstraUI permission dialog. The API is
/// stable so callers do not need to change when persistence arrives.
class PermissionDatabase {
public:
    /// Grant @p permission to @p module. Returns true if the grant is new.
    bool grant(
        const std::string& module,
        Permission permission
    );

    /// Revoke @p permission from @p module. Returns true if a grant was
    /// removed.
    bool revoke(
        const std::string& module,
        Permission permission
    );

    /// @return true iff @p module has been granted @p permission.
    bool check(
        const std::string& module,
        Permission permission
    ) const;

    /// Revoke every permission held by @p module (used on uninstall).
    void revoke_all(
        const std::string& module
    );

private:
    // module_id → set of granted permission names
    std::map<std::string, std::set<Permission>> grants_;
};

}  // namespace astra::security
