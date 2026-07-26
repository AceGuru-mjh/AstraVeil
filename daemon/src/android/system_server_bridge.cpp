#include "astra/android/system_server_bridge.hpp"

#include "astra/logger/logger.hpp"

namespace astra::android {

bool SystemServerBridge::connect() {
    ALOGI("SystemServerBridge: connect (future: binder)");
    return true;
}

bool SystemServerBridge::registerService() {
    ALOGI("SystemServerBridge: registerService (future: AstraService)");
    return true;
}

}  // namespace astra::android
