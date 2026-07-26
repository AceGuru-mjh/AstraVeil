#pragma once

#include <string>

namespace astra {

namespace provider { class ProviderRegistry; }

/// Decodes a length-framed protobuf [ExecuteRequest] and dispatches it
/// to the [execution::ExecutionPipeline] via the [ProviderRegistry].
///
/// Flow:
/// @code
/// raw bytes
///   ↓
/// Parse ExecuteRequest
///   ↓
/// ExecutionPipeline.run(capability, command)
///   ↓
/// ProviderRegistry.resolve(capability) → Provider
///   ↓
/// ExecuteResponse (serialised back to the client)
/// @endcode
class RequestHandler {
public:
    /// @param registry  the daemon's provider registry. Must outlive the handler.
    explicit RequestHandler(provider::ProviderRegistry& registry);

    /// @return true iff the request was decoded and dispatched.
    bool handle(const char* data, int size);

private:
    provider::ProviderRegistry& registry_;
};

}  // namespace astra
