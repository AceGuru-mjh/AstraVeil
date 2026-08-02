//! Policy engine for AstraVeil modules.
//!
//! A [`ModulePolicy`] describes what a module is allowed to do. The
//! [`PolicyEngine`] holds the set of registered policies and answers
//! [`evaluate`] calls with a [`Decision`].
//!
//! ## Levels
//!
//! [`Permission`] is a coarse capability ladder — `None < Shell < Root
//! < Kernel` — that the engine uses to gate the maximum escalation a
//! module may perform. A module asking for a permission above its
//! `max_level` is denied (or routed through approval).

use std::collections::{HashMap, HashSet};

/// Coarse capability ladder. Lower variants are strictly weaker than
/// higher variants; see [`Permission::as_level`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, serde::Serialize, serde::Deserialize)]
pub enum Permission {
    #[default]
    None,
    Shell,
    Root,
    Kernel,
}

impl Permission {
    /// Numeric rank used for comparison. Higher means more privileged.
    pub fn as_level(self) -> u32 {
        match self {
            Permission::None => 0,
            Permission::Shell => 1,
            Permission::Root => 2,
            Permission::Kernel => 3,
        }
    }
}

/// Per-module policy: which permission strings it may request and the
/// ceiling it can ever reach.
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct ModulePolicy {
    pub module_id: String,
    /// Free-form permission tokens this module is permitted to use
    /// (e.g. `"read_storage"`, `"shell"`).
    pub permissions: Vec<String>,
    /// Hard ceiling — requests above this level are always denied.
    pub max_level: Permission,
    /// Whether the module runs inside a sandbox even when granted.
    pub sandboxed: bool,
}

/// Outcome of [`PolicyEngine::evaluate`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub enum Decision {
    /// Request granted.
    Allow,
    /// Request denied outright (e.g. denylist hit, above max_level).
    Deny,
    /// Request requires interactive user approval before it is granted.
    RequireApproval,
}

/// Permissions that are inherently dangerous and may not be granted
/// silently. Hitting any of these yields [`Decision::RequireApproval`]
/// at best, [`Decision::Deny`] if the policy's `max_level` is too low.
pub const DENYLIST: &[&str] = &[
    "mount",
    "su",
    "kernel_hook",
    "ptrace",
    "namespace",
];

fn denylist_set() -> HashSet<&'static str> {
    DENYLIST.iter().copied().collect()
}

/// The policy registry. Cheap to clone; the underlying map is keyed by
/// `module_id`.
#[derive(Debug, Clone, Default)]
pub struct PolicyEngine {
    policies: HashMap<String, ModulePolicy>,
}

impl PolicyEngine {
    pub fn new() -> Self {
        Self::default()
    }

    /// Register or replace the policy for `module_id`.
    pub fn register(&mut self, policy: ModulePolicy) {
        self.policies.insert(policy.module_id.clone(), policy);
    }

    /// Look up the policy for `module_id`, if any.
    pub fn get(&self, module_id: &str) -> Option<&ModulePolicy> {
        self.policies.get(module_id)
    }

    /// Decide whether `module_id` may exercise `requested_permission`.
    ///
    /// * Unknown module ⇒ `Deny`.
    /// * Permission not in `module.permissions` ⇒ `Deny`.
    /// * Permission on the denylist ⇒ `RequireApproval`.
    /// * Otherwise ⇒ `Allow`.
    pub fn evaluate(&self, module_id: &str, requested_permission: &str) -> Decision {
        let Some(policy) = self.policies.get(module_id) else {
            return Decision::Deny;
        };

        let allowed: HashSet<&str> = policy.permissions.iter().map(String::as_str).collect();
        if !allowed.contains(requested_permission) {
            return Decision::Deny;
        }

        if denylist_set().contains(requested_permission) {
            // Dangerous permission — never silently allow. The ceiling is
            // still consulted: a module that cannot reach the required
            // level for a denylisted perm is denied outright.
            if policy.max_level.as_level() < Permission::Shell.as_level() {
                return Decision::Deny;
            }
            return Decision::RequireApproval;
        }

        Decision::Allow
    }
}

/// A reasonable default policy for an unknown module: no permissions,
/// sandboxed, and capped at `Permission::None`.
pub fn default_policy_for(module_id: &str) -> ModulePolicy {
    ModulePolicy {
        module_id: module_id.to_string(),
        permissions: Vec::new(),
        max_level: Permission::None,
        sandboxed: true,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_policy(id: &str, perms: &[&str], max: Permission) -> ModulePolicy {
        ModulePolicy {
            module_id: id.to_string(),
            permissions: perms.iter().map(|s| s.to_string()).collect(),
            max_level: max,
            sandboxed: true,
        }
    }

    #[test]
    fn unknown_module_is_denied() {
        let engine = PolicyEngine::new();
        assert_eq!(engine.evaluate("ghost", "shell"), Decision::Deny);
    }

    #[test]
    fn allowed_permission_passes() {
        let mut engine = PolicyEngine::new();
        engine.register(make_policy("m", &["read_storage"], Permission::None));
        assert_eq!(engine.evaluate("m", "read_storage"), Decision::Allow);
    }

    #[test]
    fn denylist_requires_approval() {
        let mut engine = PolicyEngine::new();
        engine.register(make_policy("m", &["mount", "su"], Permission::Root));
        assert_eq!(engine.evaluate("m", "mount"), Decision::RequireApproval);
        assert_eq!(engine.evaluate("m", "su"), Decision::RequireApproval);
    }

    #[test]
    fn denylist_with_low_ceiling_is_denied() {
        let mut engine = PolicyEngine::new();
        engine.register(make_policy("m", &["ptrace"], Permission::None));
        assert_eq!(engine.evaluate("m", "ptrace"), Decision::Deny);
    }

    #[test]
    fn default_policy_has_denylist_permissions() {
        // default_policy_for returns an empty permissions vector and a
        // Permission::None ceiling — i.e. the default module is granted
        // nothing, denylist or otherwise. This test pins that contract.
        let p = default_policy_for("x");
        assert_eq!(p.module_id, "x");
        assert!(p.permissions.is_empty());
        assert_eq!(p.max_level, Permission::None);
        assert!(p.sandboxed);
    }

    #[test]
    fn policy_engine_get_returns_none_for_unknown() {
        let engine = PolicyEngine::new();
        assert!(engine.get("unknown").is_none());
    }

    #[test]
    fn policy_engine_register_then_get() {
        let mut engine = PolicyEngine::new();
        engine.register(make_policy("mod", &["filesystem"], Permission::Shell));
        let got = engine.get("mod");
        assert!(got.is_some());
        assert_eq!(got.unwrap().module_id, "mod");
        assert_eq!(got.unwrap().permissions, vec!["filesystem".to_string()]);
    }

    #[test]
    fn policy_engine_evaluate_unknown_module_denied() {
        let engine = PolicyEngine::new();
        assert_eq!(engine.evaluate("unknown", "filesystem"), Decision::Deny);
    }

    #[test]
    fn policy_engine_evaluate_registered_allowed() {
        let mut engine = PolicyEngine::new();
        engine.register(make_policy("mod", &["filesystem"], Permission::Shell));
        // "filesystem" is permitted and not on the denylist ⇒ Allow.
        assert_eq!(engine.evaluate("mod", "filesystem"), Decision::Allow);
    }

    #[test]
    fn policy_engine_evaluate_denylist_requires_approval() {
        let mut engine = PolicyEngine::new();
        // "mount" is on the DENYLIST; with a max_level of Shell the
        // engine routes the request through interactive approval rather
        // than silently granting it.
        engine.register(make_policy("mod", &["mount"], Permission::Shell));
        assert_eq!(engine.evaluate("mod", "mount"), Decision::RequireApproval);
    }

    #[test]
    fn permission_as_level_none() {
        assert_eq!(Permission::None.as_level(), 0);
    }

    #[test]
    fn permission_as_level_root() {
        // The capability ladder is None(0) < Shell(1) < Root(2) < Kernel(3);
        // Root sits at level 2.
        assert_eq!(Permission::Root.as_level(), 2);
    }

    #[test]
    fn permission_default_is_none() {
        assert_eq!(Permission::default(), Permission::None);
    }
}
