//! Execution-level policy: the v3 security decision that gates every
//! root operation before it reaches a [crate::policy::PolicyEngine]
//! module-level decision.
//!
//! The flow is:
//!
//! ```text
//! ExecutionRequest
//!   ↓
//! evaluate(ExecutionPolicy)
//!   ↓
//! PolicyDecision { Allow, Deny, RequireApproval }
//! ```
//!
//! `Allow` means the request is low-risk AND pre-approved; `Deny` means
//! not approved (and not high-risk enough to prompt); `RequireApproval`
//! means high-risk — the UI must prompt the user.

use serde::{Deserialize, Serialize};

/// Three-valued policy decision returned by [evaluate].
#[derive(Serialize, Deserialize, Debug, Clone, Copy, PartialEq, Eq)]
pub enum PolicyDecision {
    /// Request is permitted without prompting.
    Allow,
    /// Request is refused; no prompt.
    Deny,
    /// Request is high-risk; the UI must prompt the user before it can be granted.
    RequireApproval,
}

/// Inputs to the execution-level policy decision.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ExecutionPolicy {
    pub module_id: String,
    pub capability: String,
    /// 0–100 risk score from the risk engine.
    pub risk_level: u32,
    /// True iff the user (or cached policy) already approved this
    /// module + capability pair.
    pub approved: bool,
}

/// Evaluate [policy] and return a [PolicyDecision].
///
/// Rules:
/// 1. `risk_level >= 80` → `RequireApproval` (high-risk always prompts,
///    even if previously approved, so the user re-confirms).
/// 2. `!approved` → `Deny` (no prior grant).
/// 3. Otherwise → `Allow`.
pub fn evaluate(policy: ExecutionPolicy) -> PolicyDecision {
    if policy.risk_level >= 80 {
        return PolicyDecision::RequireApproval;
    }
    if !policy.approved {
        return PolicyDecision::Deny;
    }
    PolicyDecision::Allow
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn high_risk_requires_approval() {
        let d = evaluate(ExecutionPolicy {
            module_id: "m".into(),
            capability: "ROOT_ACCESS".into(),
            risk_level: 90,
            approved: true,
        });
        assert_eq!(d, PolicyDecision::RequireApproval);
    }

    #[test]
    fn unapproved_denies() {
        let d = evaluate(ExecutionPolicy {
            module_id: "m".into(),
            capability: "MOUNT".into(),
            risk_level: 30,
            approved: false,
        });
        assert_eq!(d, PolicyDecision::Deny);
    }

    #[test]
    fn low_risk_approved_allows() {
        let d = evaluate(ExecutionPolicy {
            module_id: "m".into(),
            capability: "FILESYSTEM".into(),
            risk_level: 20,
            approved: true,
        });
        assert_eq!(d, PolicyDecision::Allow);
    }
}
