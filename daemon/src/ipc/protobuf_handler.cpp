#include "astra/ipc/protobuf_handler.hpp"

#include "astra/ipc/message_router.hpp"
#include "astra/logger/logger.hpp"

namespace astra {

bool ProtobufHandler::handle(const void* buffer, int size) {
    if (!buffer || size <= 0) {
        return false;
    }
    /*
     * Phase 4: the first payload byte is the request-type discriminator
     * (matching RequestType in main.cpp); the rest is the payload.
     * Once libprotobuf is linked, the payload is ParseFromArray'd into
     * the right message; for now we route by type byte.
     */
    const auto* bytes = static_cast<const std::uint8_t*>(buffer);
    const int type = bytes[0];
    MessageRouter router;
    return router.route(type, bytes + 1, size - 1);
}

}  // namespace astra
