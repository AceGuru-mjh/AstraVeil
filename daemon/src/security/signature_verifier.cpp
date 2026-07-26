#include "astra/security/signature_verifier.hpp"

#include "astra/logger/logger.hpp"

#include <filesystem>

namespace astra::security {

bool SignatureVerifier::verify(std::string module_path) {
    /*
     * 安装模块
     *   ↓
     * 读取签名 (signature.sig)
     *   ↓
     * 校验开发者证书
     *   ↓
     * 通过 → 安装
     */

    const auto sig_path =
        std::filesystem::path(module_path) / "signature.sig";

    if (!std::filesystem::exists(sig_path)) {
        ALOGW("SignatureVerifier: %s is unsigned (no signature.sig)",
              module_path.c_str());
        // Phase 7: allow unsigned modules to install but mark them as
        // untrusted. A later sub-phase will refuse unsigned modules when
        // the device policy requires signatures.
        return true;
    }

    // TODO(Phase 7.x): load the developer certificate, hash the manifest,
    // and verify the signature (Ed25519 / RSA-PSS). Until the crypto
    // primitive is wired, accept any present signature so the pipeline
    // is exercised.
    ALOGI("SignatureVerifier: %s carries a signature (verification stubbed)",
          module_path.c_str());
    return true;
}

}  // namespace astra::security
