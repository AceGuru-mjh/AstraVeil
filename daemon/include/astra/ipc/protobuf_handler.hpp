#pragma once

namespace astra {

/// Decodes a length-framed protobuf payload and hands it to the
/// [MessageRouter].
///
/// Flow:
///   socket bytes
///     ↓
///   ProtobufHandler.handle(buf, size)
///     ↓
///   (Phase 4: ParseFromArray once libprotobuf linked)
///     ↓
///   MessageRouter.route(type, payload)
class ProtobufHandler {
public:
    /// @return true iff the buffer was decoded + routed.
    bool handle(const void* buffer, int size);
};

}  // namespace astra
