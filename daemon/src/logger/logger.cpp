// logger/logger.cpp

#include "astra/logger/logger.hpp"

#include <atomic>
#include <chrono>
#include <cstdio>
#include <ctime>
#include <deque>
#include <mutex>
#include <string>

namespace astra::logger {

namespace {

std::mutex g_ring_mutex;
std::deque<std::string> g_ring;
std::atomic<bool> g_stderr_enabled{true};

const char* level_str(Level l) noexcept {
    switch (l) {
        case Level::Debug: return "D";
        case Level::Info:  return "I";
        case Level::Warn:  return "W";
        case Level::Error: return "E";
    }
    return "?";
}

/// Format a wall-clock timestamp like `2024-11-19 14:32:01.123`.
std::string timestamp() {
    using namespace std::chrono;
    const auto now = system_clock::now();
    const auto t = system_clock::to_time_t(now);
    const auto ms = duration_cast<milliseconds>(now.time_since_epoch()) % 1000;
    std::tm tm{};
    ::localtime_r(&t, &tm);
    char buf[32];
    std::snprintf(buf, sizeof(buf), "%04d-%02d-%02d %02d:%02d:%02d.%03lld",
                  tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday,
                  tm.tm_hour, tm.tm_min, tm.tm_sec,
                  static_cast<long long>(ms.count()));
    return buf;
}

/// Format a single record line. The format mirrors Android logcat:
///   <LEVEL>/<tag>: <msg>
std::string format_line(Level level, const std::string& tag, const std::string& msg) {
    std::string line;
    line.reserve(8 + tag.size() + msg.size());
    line += timestamp();
    line += ' ';
    line += level_str(level);
    line += '/';
    line += tag;
    line += ": ";
    line += msg;
    return line;
}

void push_ring(const std::string& line) {
    std::lock_guard<std::mutex> g(g_ring_mutex);
    g_ring.push_back(line);
    while (g_ring.size() > kRingCapacity) {
        g_ring.pop_front();
    }
}

}  // namespace

void log(Level level, const std::string& tag, const std::string& msg) {
    const std::string line = format_line(level, tag, msg);
    push_ring(line);
    if (g_stderr_enabled.load()) {
        std::fprintf(stderr, "%s\n", line.c_str());
        std::fflush(stderr);
    }
}

void vlogf(Level level, const char* tag, const char* fmt, ...) noexcept {
    char buf[1024];
    va_list ap;
    va_start(ap, fmt);
    const int n = std::vsnprintf(buf, sizeof(buf), fmt, ap);
    va_end(ap);
    std::string msg;
    if (n < 0) {
        msg = "<log format error>";
    } else if (static_cast<std::size_t>(n) < sizeof(buf)) {
        msg.assign(buf, static_cast<std::size_t>(n));
    } else {
        // Message longer than the stack buffer — allocate.
        msg.resize(static_cast<std::size_t>(n));
        va_start(ap, fmt);
        std::vsnprintf(msg.data(), msg.size() + 1, fmt, ap);
        va_end(ap);
    }
    log(level, tag ? tag : "astrad", msg);
}

std::vector<std::string> recent(std::size_t n) {
    std::lock_guard<std::mutex> g(g_ring_mutex);
    std::vector<std::string> out;
    if (n == 0 || n >= g_ring.size()) {
        out.assign(g_ring.begin(), g_ring.end());
    } else {
        out.assign(g_ring.end() - static_cast<std::ptrdiff_t>(n), g_ring.end());
    }
    return out;
}

}  // namespace astra::logger
