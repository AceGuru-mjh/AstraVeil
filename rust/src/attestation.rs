//! Module attestation helpers.
//!
//! Before executing a module payload the daemon hashes it and mints an
//! [`AttestationToken`] recording which module it was, when, and the
//! resulting SHA-256. The token is JSON-serialisable so it can be
//! logged, persisted, or sent back over IPC.

use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};

/// A signed-off record describing an attested module payload.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AttestationToken {
    pub module_id: String,
    /// Wall-clock time in milliseconds since the Unix epoch.
    pub timestamp: u64,
    /// Lowercase hex SHA-256 of the attested content.
    pub hash: String,
}

/// Compute the lowercase-hex SHA-256 of `data`.
pub fn sha256_hex(data: &[u8]) -> String {
    let mut hasher = Sha256::new();
    hasher.update(data);
    let digest = hasher.finalize();
    let mut out = String::with_capacity(64);
    for byte in digest {
        use std::fmt::Write;
        let _ = write!(out, "{:02x}", byte);
    }
    out
}

impl AttestationToken {
    /// Mint a token for `module_id` over `content`. `timestamp` is set
    /// to the current Unix time in milliseconds.
    pub fn for_module(module_id: impl Into<String>, content: &[u8]) -> Self {
        let timestamp = unix_millis();
        let hash = sha256_hex(content);
        Self {
            module_id: module_id.into(),
            timestamp,
            hash,
        }
    }

    /// Serialise to a JSON string.
    pub fn to_json(&self) -> Result<String, serde_json::Error> {
        serde_json::to_string(self)
    }

    /// Parse from a JSON string.
    pub fn from_json(s: &str) -> Result<Self, serde_json::Error> {
        serde_json::from_str(s)
    }
}

fn unix_millis() -> u64 {
    use std::time::{SystemTime, UNIX_EPOCH};
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sha256_known_vector() {
        // sha256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        assert_eq!(
            sha256_hex(b"abc"),
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        );
    }

    #[test]
    fn token_round_trips_json() {
        let token = AttestationToken::for_module("com.example.mod", b"hello");
        let json = token.to_json().unwrap();
        let back = AttestationToken::from_json(&json).unwrap();
        assert_eq!(token.module_id, back.module_id);
        assert_eq!(token.hash, back.hash);
        assert_eq!(token.timestamp, back.timestamp);
    }

    #[test]
    fn token_hash_matches_helper() {
        let token = AttestationToken::for_module("m", b"payload");
        assert_eq!(token.hash, sha256_hex(b"payload"));
    }
}
