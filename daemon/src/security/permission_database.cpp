#include "astra/security/permission_database.hpp"

namespace astra::security {

bool PermissionDatabase::grant(
    const std::string& module,
    Permission permission
) {
    auto& set = grants_[module];
    return set.insert(permission).second;
}

bool PermissionDatabase::revoke(
    const std::string& module,
    Permission permission
) {
    auto it = grants_.find(module);
    if (it == grants_.end()) {
        return false;
    }
    return it->second.erase(permission) > 0;
}

bool PermissionDatabase::check(
    const std::string& module,
    Permission permission
) const {
    auto it = grants_.find(module);
    if (it == grants_.end()) {
        return false;
    }
    return it->second.count(permission) > 0;
}

void PermissionDatabase::revoke_all(
    const std::string& module
) {
    grants_.erase(module);
}

}  // namespace astra::security
