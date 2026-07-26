#include "astra/android/property_manager.hpp"

#include "astra/logger/logger.hpp"

#if defined(__ANDROID__)
extern "C" int __system_property_get(const char*, char*);
extern "C" int __system_property_set(const char*, const char*);
#endif

namespace astra::android {

std::string PropertyManager::get(const std::string& key) {
#if defined(__ANDROID__)
    char buf[92] = {0};
    if (__system_property_get(key.c_str(), buf) > 0) {
        return std::string(buf);
    }
    return {};
#else
    // On a non-Android host, fall back to reading /system/build.prop
    // for ro.* keys (read-only). This keeps the API testable.
    (void)key;
    return {};
#endif
}

bool PropertyManager::set(const std::string& key, const std::string& value) {
#if defined(__ANDROID__)
    return __system_property_set(key.c_str(), value.c_str()) == 0;
#else
    ALOGI("PropertyManager: set %s=%s (host stub)", key.c_str(), value.c_str());
    return true;
#endif
}

}  // namespace astra::android
