#pragma once

#include <string>

namespace astra {

/// Result of a Rust [policy_check] call.
enum class PolicyResult {
    ALLOW,
    DENY,
    APPROVAL,
};

/// C++ bridge to the Rust execution-policy engine (`astra_rust::ffi`).
///
/// The daemon calls [check] before every root operation to ask the Rust
/// security layer whether the request should proceed. Rust is the final
/// authority — the Kotlin PermissionEngine is a fast-path cache; the
/// Rust policy is the enforced boundary.
///
/// Phase 2.3: [check] uses the default-argument FFI (`policy_check`).
/// [checkWith] passes real inputs once the FFI struct bridge is wired.
class PolicyBridge {
public:
    /// Default policy check (module_id="unknown", risk=0, approved=true).
    /// Returns ALLOW for the default policy.
    PolicyResult check();

    /// Explicit policy check. Returns the Rust decision.
    PolicyResult checkWith(
        const std::string& moduleId,
        const std::string& capability,
        unsigned int riskLevel,
        bool approved
    );
};

}  // namespace astra
