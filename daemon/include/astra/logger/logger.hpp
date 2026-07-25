#pragma once

// astra/logger/logger.hpp
//
// Daemon-side logger. Writes to stderr and keeps an in-memory ring
// buffer of recent records for inspection (e.g. via a future IPC
// `GetLogs` request).
//
// `ALOGD/ALOGI/ALOGW/ALOGE` mirror the names used by the Android native
// logging macros so daemon code is easy to port. They are printf-style.

#include <cstdarg>
#include <cstddef>
#include <string>
#include <vector>

namespace astra::logger {

enum class Level {
    Debug,
    Info,
    Warn,
    Error,
};

/// Maximum number of records kept in the ring buffer.
inline constexpr std::size_t kRingCapacity = 1024;

/// Emit a single log record (formatted by the caller).
void log(Level level, const std::string& tag, const std::string& msg);

/// printf-style helper used by the `ALOG*` macros. Defined in
/// `logger.cpp`; declared here so the macros below can call it.
void vlogf(Level level, const char* tag, const char* fmt, ...) noexcept;

/// Return the most recent `n` log records (oldest first). An `n` of 0
/// returns everything currently buffered.
std::vector<std::string> recent(std::size_t n = 0);

}  // namespace astra::logger

#define ALOGD(fmt, ...) ::astra::logger::vlogf(::astra::logger::Level::Debug, "astrad", (fmt), ##__VA_ARGS__)
#define ALOGI(fmt, ...) ::astra::logger::vlogf(::astra::logger::Level::Info,  "astrad", (fmt), ##__VA_ARGS__)
#define ALOGW(fmt, ...) ::astra::logger::vlogf(::astra::logger::Level::Warn,  "astrad", (fmt), ##__VA_ARGS__)
#define ALOGE(fmt, ...) ::astra::logger::vlogf(::astra::logger::Level::Error, "astrad", (fmt), ##__VA_ARGS__)
