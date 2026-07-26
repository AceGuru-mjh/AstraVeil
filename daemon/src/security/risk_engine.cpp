#include "astra/security/risk_engine.hpp"
#include "astra/security/permission.hpp"

namespace astra::security {

namespace {

/// Score contribution of a single permission name.
int permission_score(const std::string& name) {
    if (name == "ROOT_ACCESS")        return 30;
    if (name == "BOOT_PATCH")         return 40;
    if (name == "NETWORK_ACCESS")     return 10;
    if (name == "SYSTEM_WRITE")       return 20;
    if (name == "SELINUX_CONTROL")    return 25;
    if (name == "KERNEL_INTERFACE")   return 35;
    if (name == "MOUNT")              return 10;
    if (name == "FILESYSTEM_ACCESS")  return 10;
    if (name == "IPC_ACCESS")          return 5;
    return 0;
}

}  // namespace

int RiskEngine::calculate(
    const astra::module::ModuleManifest& module
) const {
    int score = 0;
    for (const auto& perm : module.permissions) {
        score += permission_score(perm.name);
    }
    return score;
}

std::string RiskEngine::level(
    const astra::module::ModuleManifest& module
) const {
    return level_for_score(calculate(module));
}

std::string RiskEngine::level_for_score(int score) {
    if (score >= 100) return "CRITICAL";
    if (score >= 60)  return "HIGH";
    if (score >= 30)  return "MEDIUM";
    return "LOW";
}

}  // namespace astra::security
