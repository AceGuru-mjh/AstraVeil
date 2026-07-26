//! FFI surface for the daemon (C++) to call the Rust policy engine.
//!
//! The daemon links `libastra_rust.a` and calls [policy_check] before
//! every root operation. The C side sees an `i32`:
//!
//! ```text
//! 0 = Allow
//! 1 = Deny
//! 2 = RequireApproval
//! ```
//!
//! Phase 2.3 ships a default-constructed policy (module_id="unknown",
//! risk_level=0, approved=true → Allow) so the pipeline can be wired
//! end to end. A later sub-phase will pass the real `ExecutionPolicy`
//! struct across the FFI boundary (via a serialised JSON blob or a
//! #[repr(C)] struct).

use crate::execution_policy::{evaluate, ExecutionPolicy, PolicyDecision};

/// C ABI policy check. Returns 0/1/2 — see module docs.
#[no_mangle]
pub extern "C" fn policy_check() -> i32 {
    let result = evaluate(ExecutionPolicy {
        module_id: "unknown".into(),
        capability: "unknown".into(),
        risk_level: 0,
        approved: true,
    });
    match result {
        PolicyDecision::Allow => 0,
        PolicyDecision::Deny => 1,
        PolicyDecision::RequireApproval => 2,
    }
}

/// C ABI policy check with explicit inputs. Returns 0/1/2.
///
/// # Safety
/// `module_id` and `capability` must be valid null-terminated C strings.
#[no_mangle]
pub unsafe extern "C" fn policy_check_with(
    module_id: *const std::os::raw::c_char,
    capability: *const std::os::raw::c_char,
    risk_level: u32,
    approved: bool,
) -> i32 {
    let module_id = if module_id.is_null() {
        "unknown".to_string()
    } else {
        std::ffi::CStr::from_ptr(module_id).to_string_lossy().into_owned()
    };
    let capability = if capability.is_null() {
        "unknown".to_string()
    } else {
        std::ffi::CStr::from_ptr(capability).to_string_lossy().into_owned()
    };
    let result = evaluate(ExecutionPolicy {
        module_id,
        capability,
        risk_level,
        approved,
    });
    match result {
        PolicyDecision::Allow => 0,
        PolicyDecision::Deny => 1,
        PolicyDecision::RequireApproval => 2,
    }
}
