#include "astra/security/policy_bridge.hpp"

#include "astra/logger/logger.hpp"

// Rust FFI — declared in rust/src/ffi.rs, linked from libastra_rust.a.
//
// P0-2 fix: weak fallbacks are now DENY (fail-closed), not ALLOW.
// A new weak symbol policy_is_available() returns 0 by default
// (meaning "Rust NOT linked"). The real libastra_rust.a overrides
// it to return 1. PolicyBridge checks this before calling policy_check.
extern "C" {

/// Weak default: 0 = Rust NOT linked. Overridden by libastra_rust.a.
__attribute__((weak)) int policy_is_available() {
    return 0;
}

/// Weak default: DENY (1). Overridden by libastra_rust.a when linked.
__attribute__((weak)) int policy_check() {
    return 1;  // Deny (fail closed)
}

/// Weak default: DENY (1). Overridden by libastra_rust.a when linked.
__attribute__((weak)) int policy_check_with(
    const char* /*module_id*/,
    const char* /*capability*/,
    unsigned int /*risk_level*/,
    bool /*approved*/
) {
    return 1;  // Deny (fail closed)
}

}  // extern "C"

namespace astra {

namespace {

PolicyResult decode(int raw) {
    switch (raw) {
        case 0:  return PolicyResult::ALLOW;
        case 1:  return PolicyResult::DENY;
        default: return PolicyResult::APPROVAL;
    }
}

}  // namespace

PolicyResult PolicyBridge::check() {
    // P0-2 fix: fail closed if Rust policy engine is not linked
    if (::policy_is_available() == 0) {
        ALOGE("PolicyBridge: Rust policy engine NOT linked — FAILING CLOSED (deny). "
              "This is a release build misconfiguration.");
        return PolicyResult::DENY;
    }
    const int raw = ::policy_check();
    const auto result = decode(raw);
    ALOGI("PolicyBridge: check() -> %d (%s)", raw,
          result == PolicyResult::ALLOW ? "ALLOW"
          : result == PolicyResult::DENY ? "DENY" : "APPROVAL");
    return result;
}

PolicyResult PolicyBridge::checkWith(
    const std::string& moduleId,
    const std::string& capability,
    unsigned int riskLevel,
    bool approved
) {
    // P0-2 fix: fail closed if Rust policy engine is not linked
    if (::policy_is_available() == 0) {
        ALOGE("PolicyBridge: Rust NOT linked — FAILING CLOSED for "
              "checkWith(%s,%s). Release misconfiguration.",
              moduleId.c_str(), capability.c_str());
        return PolicyResult::DENY;
    }
    const int raw = ::policy_check_with(
        moduleId.c_str(), capability.c_str(), riskLevel, approved);
    const auto result = decode(raw);
    ALOGI("PolicyBridge: checkWith(%s,%s,risk=%u,approved=%d) -> %s",
          moduleId.c_str(), capability.c_str(), riskLevel, approved ? 1 : 0,
          result == PolicyResult::ALLOW ? "ALLOW"
          : result == PolicyResult::DENY ? "DENY" : "APPROVAL");
    return result;
}

}  // namespace astra
