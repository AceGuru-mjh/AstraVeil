#pragma once

// logger_native.h
//
// Logging helpers for the AstraVeil native bridge. Wraps
// `__android_log_print` and exposes `LOGD/LOGI/LOGW/LOGE` macros.

#include <android/log.h>

namespace astra {

/// Emit a log record through `__android_log_print`. `level` follows the
/// Android log priority constants (`ANDROID_LOG_DEBUG` etc.).
void astra_log(int level, const char* tag, const char* msg);

}  // namespace astra

#ifndef ASTRA_LOG_TAG
#define ASTRA_LOG_TAG "AstraNative"
#endif

#define LOGD(fmt, ...) \
    __android_log_print(ANDROID_LOG_DEBUG, ASTRA_LOG_TAG, fmt, ##__VA_ARGS__)
#define LOGI(fmt, ...) \
    __android_log_print(ANDROID_LOG_INFO, ASTRA_LOG_TAG, fmt, ##__VA_ARGS__)
#define LOGW(fmt, ...) \
    __android_log_print(ANDROID_LOG_WARN, ASTRA_LOG_TAG, fmt, ##__VA_ARGS__)
#define LOGE(fmt, ...) \
    __android_log_print(ANDROID_LOG_ERROR, ASTRA_LOG_TAG, fmt, ##__VA_ARGS__)
