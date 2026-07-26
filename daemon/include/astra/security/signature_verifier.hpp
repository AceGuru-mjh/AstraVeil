#pragma once

#include <string>

namespace astra::security {

/// Verifies the cryptographic signature of an AVM module package.
///
/// AVM v2 packages carry a `signature.sig` alongside `manifest.json`:
/// @code
/// example.avm
/// ├── manifest.json
/// ├── module.so
/// ├── assets/
/// └── signature.sig
/// @endcode
///
/// Verification flow:
/// @code
/// 安装模块
///   ↓
/// 读取签名 (signature.sig)
///   ↓
/// 校验开发者证书
///   ↓
/// 通过 → 安装
/// @endcode
///
/// Phase 7 skeleton: real signature verification (Ed25519 / RSA-PSS over
/// the manifest hash) lands alongside the developer certificate store in
/// a later sub-phase. For now [verify] returns true so the install path
/// is exercised end-to-end; the AstraUI Security panel will show
/// "unsigned" until a certificate is present.
class SignatureVerifier {
public:
    /// @param module_path  Path to the unpacked module directory
    ///                      (containing manifest.json + signature.sig).
    /// @return true if the signature is present and valid; false if the
    ///         signature is missing or does not verify.
    bool verify(
        std::string module_path
    );
};

}  // namespace astra::security
