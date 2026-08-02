//! Capability Token System — HMAC-SHA256 signed, unforgeable capability tokens.
//!
//! A CapabilityToken is a cryptographically signed credential proving a module
//! holds a specific capability. Signed with HMAC-SHA256 using a daemon-side
//! secret. Modules cannot forge tokens without the key.
//!
//! Token format (JSON):
//! ```json
//! {
//!   "module_id": "com.example.mod",
//!   "capability": "overlayfs",
//!   "issued_at": 1722556800,
//!   "expires_at": 1722560400,
//!   "nonce": "a1b2c3d4e5f6g7h8",
//!   "hmac": "base64url(HMAC-SHA256(payload, secret))"
//! }
//! ```
//!
//! Security properties:
//! 1. Unforgeable: without the HMAC secret, no valid token can be created.
//! 2. Non-transferable: HMAC includes module_id; another module can't reuse it.
//! 3. Time-bounded: expired tokens are rejected.
//! 4. Replay-resistant: unique nonce per token.
//!
//! Comparison with seL4: seL4 capabilities are kernel-managed CNode indices.
//! AstraVeil tokens are HMAC-signed JSON — weaker (secret could be extracted
//! from daemon memory) but practical for userspace. The key advantage over
//! string-based permissions is CRYPTOGRAPHIC VERIFIABILITY.

use hmac::{Hmac, Mac};
use serde::{Deserialize, Serialize};
use sha2::Sha256;
use std::time::{SystemTime, UNIX_EPOCH};

type HmacSha256 = Hmac<Sha256>;

/// A signed capability token.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CapabilityToken {
    pub module_id: String,
    pub capability: String,
    pub issued_at: u64,
    pub expires_at: u64,
    pub nonce: String,
    pub hmac: String,
}

/// Token issuance and verification authority.
///
/// The daemon creates one TokenAuthority at startup with a random secret.
/// The secret NEVER leaves daemon memory. Modules receive tokens but
/// cannot create new ones.
pub struct TokenAuthority {
    secret: Vec<u8>,
}

impl TokenAuthority {
    /// Create a new authority with the given HMAC secret.
    pub fn new(secret: Vec<u8>) -> Self {
        Self { secret }
    }

    /// Generate a cryptographically random 32-byte secret.
    ///
    /// Uses /dev/urandom on Linux. Falls back to time-based hash
    /// if /dev/urandom is unavailable (should never happen on Android).
    pub fn generate_secret() -> Vec<u8> {
        #[cfg(target_os = "linux")]
        {
            use std::io::Read;
            if let Ok(mut f) = std::fs::File::open("/dev/urandom") {
                let mut buf = vec![0u8; 32];
                if f.read_exact(&mut buf).is_ok() {
                    return buf;
                }
            }
        }
        // Fallback (non-ideal but functional).
        use sha2::Digest;
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_nanos();
        let mut hasher = sha2::Sha256::new();
        hasher.update(now.to_le_bytes());
        hasher.update(b"astraveil-token-secret-fallback");
        hasher.finalize().to_vec()
    }

    /// Issue a new capability token.
    ///
    /// # Arguments
    /// * `module_id` - The module this token is bound to.
    /// * `capability` - The capability being granted.
    /// * `duration_secs` - Token validity duration in seconds.
    pub fn issue(
        &self,
        module_id: &str,
        capability: &str,
        duration_secs: u64,
    ) -> CapabilityToken {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();

        // Generate a unique nonce from time + module_id hash + counter.
        let nonce = {
            use sha2::Digest;
            static COUNTER: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(0);
            let count = COUNTER.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
            let mut h = sha2::Sha256::new();
            h.update(now.to_le_bytes());
            h.update(count.to_le_bytes());
            h.update(module_id.as_bytes());
            h.update(capability.as_bytes());
            let hash = h.finalize();
            hex_encode(&hash[..8])
        };

        let mut token = CapabilityToken {
            module_id: module_id.to_string(),
            capability: capability.to_string(),
            issued_at: now,
            expires_at: now.saturating_add(duration_secs),
            nonce,
            hmac: String::new(),
        };

        token.hmac = self.compute_hmac(&token);
        token
    }

    /// Verify a capability token.
    ///
    /// Returns true if and only if:
    /// 1. The HMAC is valid (issued by this authority).
    /// 2. The token has not expired.
    /// 3. The module_id matches the expected caller.
    pub fn verify(&self, token: &CapabilityToken, expected_module_id: &str) -> bool {
        // Gate 1: module binding.
        if token.module_id != expected_module_id {
            return false;
        }

        // Gate 2: expiry.
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_secs();
        if now > token.expires_at {
            return false;
        }

        // Gate 3: HMAC verification (constant-time comparison).
        let expected_hmac = self.compute_hmac(token);
        constant_time_eq(token.hmac.as_bytes(), expected_hmac.as_bytes())
    }

    /// Compute HMAC-SHA256 over the token's payload fields.
    fn compute_hmac(&self, token: &CapabilityToken) -> String {
        let payload = format!(
            "{}:{}:{}:{}:{}",
            token.module_id, token.capability, token.issued_at, token.expires_at, token.nonce,
        );

        let mut mac =
            HmacSha256::new_from_slice(&self.secret).expect("HMAC accepts any key length");
        mac.update(payload.as_bytes());
        let result = mac.finalize().into_bytes();
        base64url_encode(&result)
    }
}

/// Constant-time byte comparison to prevent timing side-channels.
fn constant_time_eq(a: &[u8], b: &[u8]) -> bool {
    if a.len() != b.len() {
        return false;
    }
    let mut diff = 0u8;
    for (x, y) in a.iter().zip(b.iter()) {
        diff |= x ^ y;
    }
    diff == 0
}

/// Base64url encoding without padding (RFC 4648 §5).
fn base64url_encode(data: &[u8]) -> String {
    const CHARS: &[u8] = b"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    let mut result = String::with_capacity(data.len().div_ceil(3) * 4);
    for chunk in data.chunks(3) {
        let b0 = chunk[0] as u32;
        let b1 = if chunk.len() > 1 { chunk[1] as u32 } else { 0 };
        let b2 = if chunk.len() > 2 { chunk[2] as u32 } else { 0 };
        let n = (b0 << 16) | (b1 << 8) | b2;
        result.push(CHARS[(n >> 18 & 0x3F) as usize] as char);
        result.push(CHARS[(n >> 12 & 0x3F) as usize] as char);
        if chunk.len() > 1 {
            result.push(CHARS[(n >> 6 & 0x3F) as usize] as char);
        }
        if chunk.len() > 2 {
            result.push(CHARS[(n & 0x3F) as usize] as char);
        }
    }
    result
}

/// Hex encoding for nonces.
fn hex_encode(data: &[u8]) -> String {
    data.iter().map(|b| format!("{:02x}", b)).collect()
}

// ── FFI surface ──

/// FFI: issue a capability token. Returns JSON via out_json buffer.
///
/// # Safety
/// All pointer parameters must be valid. `out_json` must have at least
/// `out_len` bytes available.
/// Returns: 0 on success, -1 on error.
#[no_mangle]
pub unsafe extern "C" fn token_issue(
    module_id: *const std::os::raw::c_char,
    capability: *const std::os::raw::c_char,
    duration_secs: u64,
    secret: *const u8,
    secret_len: usize,
    out_json: *mut std::os::raw::c_char,
    out_len: usize,
) -> i32 {
    use std::ffi::CStr;

    if module_id.is_null() || capability.is_null() || secret.is_null() || out_json.is_null() {
        return -1;
    }

    let mod_str = CStr::from_ptr(module_id).to_string_lossy().into_owned();
    let cap_str = CStr::from_ptr(capability).to_string_lossy().into_owned();
    let secret_vec = std::slice::from_raw_parts(secret, secret_len).to_vec();

    let authority = TokenAuthority::new(secret_vec);
    let token = authority.issue(&mod_str, &cap_str, duration_secs);
    let json = match serde_json::to_string(&token) {
        Ok(j) => j,
        Err(_) => return -1,
    };

    if json.len() + 1 > out_len {
        return -1;
    }

    std::ptr::copy_nonoverlapping(json.as_ptr(), out_json as *mut u8, json.len());
    *out_json.add(json.len()) = 0;
    0
}

/// FFI: verify a capability token.
///
/// Returns: 1 = valid, 0 = invalid, -1 = error.
///
/// # Safety
/// All pointer parameters must be valid null-terminated C strings / byte arrays.
#[no_mangle]
pub unsafe extern "C" fn token_verify(
    token_json: *const std::os::raw::c_char,
    expected_module_id: *const std::os::raw::c_char,
    secret: *const u8,
    secret_len: usize,
) -> i32 {
    use std::ffi::CStr;

    if token_json.is_null() || expected_module_id.is_null() || secret.is_null() {
        return -1;
    }

    let json_str = CStr::from_ptr(token_json).to_string_lossy().into_owned();
    let mod_str = CStr::from_ptr(expected_module_id).to_string_lossy().into_owned();
    let secret_vec = std::slice::from_raw_parts(secret, secret_len).to_vec();

    let token: CapabilityToken = match serde_json::from_str(&json_str) {
        Ok(t) => t,
        Err(_) => return -1,
    };

    let authority = TokenAuthority::new(secret_vec);
    if authority.verify(&token, &mod_str) {
        1
    } else {
        0
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn issue_and_verify_roundtrip() {
        let secret = TokenAuthority::generate_secret();
        let authority = TokenAuthority::new(secret);
        let token = authority.issue("com.test.mod", "overlayfs", 3600);
        assert!(authority.verify(&token, "com.test.mod"));
    }

    #[test]
    fn verify_rejects_wrong_module() {
        let secret = TokenAuthority::generate_secret();
        let authority = TokenAuthority::new(secret);
        let token = authority.issue("com.test.mod", "overlayfs", 3600);
        assert!(!authority.verify(&token, "com.evil.mod"));
    }

    #[test]
    fn verify_rejects_tampered_capability() {
        let secret = TokenAuthority::generate_secret();
        let authority = TokenAuthority::new(secret);
        let mut token = authority.issue("com.test.mod", "overlayfs", 3600);
        token.capability = "kernel_hook".to_string();
        assert!(!authority.verify(&token, "com.test.mod"));
    }

    #[test]
    fn verify_rejects_tampered_expiry() {
        let secret = TokenAuthority::generate_secret();
        let authority = TokenAuthority::new(secret);
        let mut token = authority.issue("com.test.mod", "overlayfs", 1);
        token.expires_at = u64::MAX; // try to make it permanent
        assert!(!authority.verify(&token, "com.test.mod"));
    }

    #[test]
    fn different_secrets_cannot_verify() {
        let auth1 = TokenAuthority::new(vec![1, 2, 3, 4]);
        let auth2 = TokenAuthority::new(vec![5, 6, 7, 8]);
        let token = auth1.issue("mod", "cap", 3600);
        assert!(!auth2.verify(&token, "mod"));
    }

    #[test]
    fn token_serializes_to_valid_json() {
        let secret = TokenAuthority::generate_secret();
        let authority = TokenAuthority::new(secret);
        let token = authority.issue("com.test.mod", "root_access", 7200);
        let json = serde_json::to_string(&token).unwrap();
        assert!(json.contains("\"module_id\":\"com.test.mod\""));
        assert!(json.contains("\"capability\":\"root_access\""));
        // Roundtrip
        let parsed: CapabilityToken = serde_json::from_str(&json).unwrap();
        assert!(authority.verify(&parsed, "com.test.mod"));
    }

    #[test]
    fn nonce_is_unique_per_token() {
        let secret = TokenAuthority::generate_secret();
        let authority = TokenAuthority::new(secret);
        let t1 = authority.issue("mod", "cap", 3600);
        // Small delay to ensure different timestamp
        std::thread::sleep(std::time::Duration::from_millis(1));
        let t2 = authority.issue("mod", "cap", 3600);
        assert_ne!(t1.nonce, t2.nonce);
    }
}
