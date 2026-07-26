#pragma once

#include <string>

namespace astra::provider {

class ProviderManager;

}  // namespace astra::provider

namespace astra::execution {

/// v3 daemon execution pipeline.
///
/// Replaces the direct `IPC → CommandExecutor` path with a layered
/// pipeline that routes by capability and goes through the provider
/// runtime:
///
/// @code
/// Request
///   ↓
/// ExecutionPipeline
///   ↓
/// ProviderManager
///   ↓
/// RootProvider
///   ↓
/// Root Backend
/// @endcode
///
/// Phase-2 skeleton: [run] delegates to a [provider::ProviderManager]
/// reference. The permission / risk / audit gates will be inserted
/// between the request and the provider in Phase 5.
class ExecutionPipeline {
public:
    /// @param manager  the daemon's ProviderManager. Must outlive the pipeline.
    explicit ExecutionPipeline(provider::ProviderManager& manager);

    /// Execute @p command under capability @p capability.
    /// Returns true on success.
    bool run(
        const std::string& capability,
        const std::string& command
    );

private:
    provider::ProviderManager& manager_;
};

}  // namespace astra::execution
