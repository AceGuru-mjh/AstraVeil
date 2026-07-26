#include "astra/service/permission_service.hpp"

#include "astra/provider/provider_manager.hpp"
#include "astra/provider/root_provider.hpp"

namespace astra::service {

PermissionService::PermissionService(
    provider::ProviderManager& manager
) : manager_(manager) {}

bool PermissionService::can_execute(
    const std::string& /*command*/
) const {
    /*
     * Delegate to the active RootProvider. When NoRootProvider is
     * active (available() == false) all execution is denied — this is
     * the Phase-1 default. Once Magisk / KernelSU / APatch /
     * AstraRoot providers are detected, available() returns true and
     * execution is permitted.
     *
     * Future: per-command policy enforcement will live here.
     */
    auto* p = manager_.current();
    return p && p->available();
}

std::string PermissionService::provider_name() const {
    auto* p = manager_.current();
    return p ? p->name() : "none";
}

}  // namespace astra::service
