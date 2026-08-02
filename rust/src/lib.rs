//! # astra_rust
//!
//! The Rust security component of AstraVeil. This crate is compiled to a
//! static library (`crate-type = ["staticlib", "rlib"]`) and linked into the
//! Android native bridge (`AstraVeil/native`) via `cargo-ndk`.
//!
//! It is organised into three submodules:
//!
//! * [`policy`] — a small policy engine that maps module permission
//!   requests to [`policy::Decision`]s using a registered set of
//!   [`policy::ModulePolicy`] rules and a denylist of dangerous
//!   permissions.
//! * [`sandbox`] — declarative sandbox configuration describing the
//!   future mount-namespace based execution environment. The actual
//!   `unshare` / `mount --bind` plumbing lives in the C++ daemon today;
//!   this module models the configuration and validates it.
//! * [`attestation`] — content-hashing and token helpers used to attest
//!   module payloads before execution.
//!
//! The crate deliberately has a minimal dependency surface (`serde`,
//! `serde_json`, `sha2`) so that cross-compilation with `cargo-ndk` is
//! fast and reproducible.

pub mod attestation;
pub mod capability_token;
pub mod execution_policy;
pub mod ffi;
pub mod policy;
pub mod sandbox;
pub mod zk_capability;

/// Static version string of the `astra_rust` crate.
pub fn version() -> &'static str {
    "0.1.0"
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn version_returns_0_1_0() {
        assert_eq!(version(), "0.1.0");
    }
}
