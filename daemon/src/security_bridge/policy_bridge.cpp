#include "astra/security/policy_bridge.hpp"

#include "astra/logger/logger.hpp"

// Rust FFI — declared in rust/src/ffi.rs, linked from libastra_rust.a.
extern "C" int policy_check();
extern "C" int policy_check_with(
    const char* module_id,
    const char* capability,
    unsigned int risk_level,
    bool approved
);

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
