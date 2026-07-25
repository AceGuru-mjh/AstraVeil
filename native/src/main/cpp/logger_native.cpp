// logger_native.cpp
//
// Thin wrapper around `__android_log_print` exposing a `LOGD/LOGI/LOGW/LOGE`
// macro set used throughout the native bridge. Keeping the implementation in
// a single TU keeps log tag strings consistent.

#include "logger_native.h"

#include <android/log.h>

namespace astra {

// Mirror of the ANDROID_LOG_* priorities used by `__android_log_print`.
// `astra_log` is exposed for callers that want to pass a runtime level.
void astra_log(int level, const char* tag, const char* msg) {
    // Clamp to known log levels. ANDROID_LOG_UNKNOWN=0 .. ANDROID_LOG_SILENT=8.
    if (level < 0) level = 0;
    if (level > 7) level = 7;
    __android_log_print(level, tag ? tag : "AstraNative", "%s", msg ? msg : "");
}

}  // namespace astra
