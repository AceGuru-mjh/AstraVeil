#pragma once

namespace astra::sdk {

/// Runtime constants exposed to AVM modules.
namespace runtime {
    /// The AVM Module API level implemented by this SDK. Modules
    /// declare `api_version` in their manifest; the daemon refuses to
    /// load a module whose api_version is greater than this.
    constexpr int API_LEVEL = 2;

    /// AVM package file extension.
    constexpr const char* MODULE_EXTENSION = ".avm";
}  // namespace runtime

}  // namespace astra::sdk
