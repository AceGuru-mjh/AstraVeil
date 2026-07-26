#pragma once

namespace astra {

/// Routes a decoded IPC request to the right handler.
///
/// Phase 4: receives a request-type byte + payload and dispatches:
///   ExecuteRequest  → ExecutionHandler
///   HeartbeatRequest → heartbeat response
///   CapabilityRequest → CapabilityService
///   ModuleRequest   → ModuleRuntime
///
/// The router is the single switch the IPC server calls; adding a new
/// request type is a one-case edit here.
class MessageRouter {
public:
    /// @param type    request-type byte (matches [RequestType] in main.cpp)
    /// @param data    payload bytes
    /// @param size    payload length
    /// @return true iff the request was routed successfully.
    bool route(int type, const void* data, int size);
};

}  // namespace astra
