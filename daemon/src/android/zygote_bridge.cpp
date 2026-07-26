#include "astra/android/zygote_bridge.hpp"

#include "astra/logger/logger.hpp"

namespace astra::android {

bool ZygoteBridge::connect() {
    ALOGI("ZygoteBridge: connect (future: binder to zygote)");
    return true;
}

bool ZygoteBridge::registerHook() {
    ALOGI("ZygoteBridge: registerHook (future: framework hook injection)");
    return true;
}

bool ZygoteBridge::removeHook() {
    ALOGI("ZygoteBridge: removeHook");
    return true;
}

}  // namespace astra::android
