//! Declarative sandbox configuration.
//!
//! Today the actual sandbox setup (`unshare(CLONE_NEWNS)`,
//! `mount --bind`, dropping capabilities) is performed by the C++
//! daemon's `astra::sandbox::Sandbox` class. This module mirrors the
//! configuration on the Rust side so that policy decisions and
//! attestation can reason about the intended execution environment
//! without depending on C++.
//!
//! ## Plan
//!
//! The end goal is for `astra_rust` to produce a fully-specified
//! [`SandboxConfig`] from a [`crate::policy::ModulePolicy`], hand it
//! to the daemon over IPC, and have the daemon apply it. The fields
//! below describe the inputs to that flow:
//!
//! * `root_dir` — the chroot/overlay root for the sandboxed process.
//! * `read_only_paths` — bind-mounted read-only.
//! * `write_paths` — paths the sandboxed module may write to.
//! * `network` — whether any network access is permitted.
//! * `max_level` — the [`crate::policy::Permission`] ceiling inside
//!   the sandbox.

use crate::policy::Permission;
use std::path::Path;

/// Declarative description of how a module should be sandboxed.
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct SandboxConfig {
    pub root_dir: String,
    pub read_only_paths: Vec<String>,
    pub write_paths: Vec<String>,
    pub network: bool,
    pub max_level: Permission,
}

impl Default for SandboxConfig {
    fn default() -> Self {
        Self {
            root_dir: "/data/astra/sandbox".to_string(),
            read_only_paths: Vec::new(),
            write_paths: Vec::new(),
            network: false,
            max_level: Permission::None,
        }
    }
}

/// Wrapper that validates a [`SandboxConfig`] before it is applied.
#[derive(Debug, Clone)]
pub struct Sandbox {
    pub config: SandboxConfig,
}

impl Sandbox {
    pub fn new(config: SandboxConfig) -> Self {
        Self { config }
    }

    /// Sanity-check the config. Currently checks that the root dir is
    /// non-empty, absolute, and that no write path overlaps a
    /// read-only path.
    pub fn validate(&self) -> Result<(), String> {
        if self.config.root_dir.trim().is_empty() {
            return Err("sandbox root_dir must not be empty".into());
        }
        if !Path::new(&self.config.root_dir).is_absolute() {
            return Err(format!(
                "sandbox root_dir must be absolute: {}",
                self.config.root_dir
            ));
        }
        let ro: std::collections::HashSet<&str> =
            self.config.read_only_paths.iter().map(String::as_str).collect();
        for w in &self.config.write_paths {
            if ro.contains(w.as_str()) {
                return Err(format!(
                    "path listed in both read_only_paths and write_paths: {w}"
                ));
            }
        }
        Ok(())
    }

    /// Human-readable description suitable for logging.
    pub fn describe(&self) -> String {
        format!(
            "Sandbox(root={}, ro={}, rw={}, net={}, max_level={})",
            self.config.root_dir,
            self.config.read_only_paths.len(),
            self.config.write_paths.len(),
            self.config.network,
            self.config.max_level.as_level(),
        )
    }
}

/// Compact, AVM-facing sandbox policy.
///
/// While [`SandboxConfig`] is the full daemon-side description (root
/// dir, path lists, permission ceiling), [`SandboxPolicy`] is the
/// simpler declaration that appears inside an AVM module's manifest
/// `sandbox` block and that the Rust policy engine reasons about. The
/// daemon's `Sandbox` class is built by expanding a `SandboxPolicy`
/// into a full `SandboxConfig`.
///
/// Fields:
/// * `filesystem` — one of `"readonly"`, `"restricted"`, `"none"`.
/// * `network` — whether the module may open sockets.
/// * `root` — whether the module may escalate to uid 0 (only honoured
///   when the device capability matrix reports `ROOT_ACCESS`).
#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
pub struct SandboxPolicy {
    pub filesystem: String,
    pub network: bool,
    pub root: bool,
}

impl SandboxPolicy {
    /// The default restricted policy applied to every AVM module
    /// unless its manifest explicitly requests otherwise: read-only
    /// filesystem, no network, no root.
    pub fn restricted() -> Self {
        Self {
            filesystem: "readonly".into(),
            network: false,
            root: false,
        }
    }

    /// Maximum-lockdown policy: no filesystem access at all, no
    /// network, no root. Used for untrusted or unsigned modules.
    pub fn deny_all() -> Self {
        Self {
            filesystem: "none".into(),
            network: false,
            root: false,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn empty_root_is_invalid() {
        let s = Sandbox::new(SandboxConfig {
            root_dir: "".into(),
            ..SandboxConfig::default()
        });
        assert!(s.validate().is_err());
    }

    #[test]
    fn relative_root_is_invalid() {
        let s = Sandbox::new(SandboxConfig {
            root_dir: "relative/path".into(),
            ..SandboxConfig::default()
        });
        assert!(s.validate().is_err());
    }

    #[test]
    fn overlap_is_invalid() {
        let s = Sandbox::new(SandboxConfig {
            root_dir: "/data/astra/sandbox".into(),
            read_only_paths: vec!["/system".into()],
            write_paths: vec!["/system".into()],
            ..SandboxConfig::default()
        });
        assert!(s.validate().is_err());
    }

    #[test]
    fn valid_config_describes() {
        let s = Sandbox::new(SandboxConfig {
            root_dir: "/data/astra/sandbox".into(),
            read_only_paths: vec!["/system".into()],
            write_paths: vec!["/data/data/com.example".into()],
            network: true,
            max_level: Permission::Shell,
            ..SandboxConfig::default()
        });
        assert!(s.validate().is_ok());
        assert!(s.describe().contains("net=true"));
    }

    #[test]
    fn sandbox_restricted_is_readonly() {
        assert_eq!(SandboxPolicy::restricted().filesystem, "readonly");
    }

    #[test]
    fn sandbox_restricted_no_network() {
        assert!(!SandboxPolicy::restricted().network);
    }

    #[test]
    fn sandbox_restricted_no_root() {
        assert!(!SandboxPolicy::restricted().root);
    }

    #[test]
    fn sandbox_deny_all_filesystem_none() {
        assert_eq!(SandboxPolicy::deny_all().filesystem, "none");
    }

    #[test]
    fn sandbox_deny_all_no_network() {
        assert!(!SandboxPolicy::deny_all().network);
    }

    #[test]
    fn sandbox_deny_all_no_root() {
        assert!(!SandboxPolicy::deny_all().root);
    }

    #[test]
    fn sandbox_custom_policy() {
        // A hand-built policy that opts into read-write filesystem and
        // network but still forbids root: this exercises the public
        // struct constructor without relying on the canned presets.
        let p = SandboxPolicy {
            filesystem: "readwrite".to_string(),
            network: true,
            root: false,
        };
        assert_eq!(p.filesystem, "readwrite");
        assert!(p.network);
        assert!(!p.root);
    }
}
