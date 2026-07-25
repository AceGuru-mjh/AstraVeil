#pragma once

// astra/service/provider_service.hpp
//
// Detects which root provider (if any) is active on the device. The
// detection is filesystem-only today — we look for the well-known
// control directories each provider leaves under /data/adb.

#include <string>

namespace astra::service {

class ProviderService {
public:
    ProviderService() = default;

    /// Returns JSON: `{"provider":"magisk"|"kernelsu"|"apatch"|"astraroot"|"none",
    ///                  "version":"unknown"}`.
    ///
    /// `version` is always "unknown" today; once we shell out to each
    /// provider's version-query tool this will be populated.
    std::string detect_provider() const;

    /// Same as `detect_provider()` but returns just the provider name.
    std::string detect_provider_name() const;
};

}  // namespace astra::service
