#include "astra/ipc/message_router.hpp"

#include "astra/logger/logger.hpp"

namespace astra {

bool MessageRouter::route(int type, const void* data, int size) {
    /*
     * Phase 4 skeleton: log the route. Real dispatch wires to
     * ExecutionHandler / CapabilityService / ModuleRuntime once the
     * protobuf decoder is linked. The request-type bytes match the
     * RequestType enum in main.cpp.
     */
    (void)data;
    (void)size;
    ALOGI("MessageRouter: route type=0x%02x size=%d", type, size);
    switch (type) {
        case 0x01:  // GetCapability
        case 0x02:  // GetProvider
        case 0x03:  // Execute
        case 0x04:  // Ping
        case 0x05:  // GetCapabilityMatrix
        case 0x0B:  // QueryPermission
            return true;
        case 0x10:  // Heartbeat
            return true;
        default:
            ALOGW("MessageRouter: unknown type 0x%02x", type);
            return false;
    }
}

}  // namespace astra
