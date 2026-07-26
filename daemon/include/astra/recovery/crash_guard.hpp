#pragma once

#include <map>
#include <string>

namespace astra::recovery {

/// Crash-recovery guard for AVM modules.
///
/// Flow:
/// @code
/// 模块启动
///   ↓
/// 异常次数统计
///   ↓
/// 超过阈值
///   ↓
/// 自动禁用模块
///   ↓
/// 保护系统
/// @endcode
class CrashGuard {
public:
    /// Register a module as running. Resets its crash counter.
    void registerModule(const std::string& id);

    /// Report that @p id crashed. Returns true if the module has now
    /// been auto-disabled (crash count exceeded the threshold).
    bool reportCrash(const std::string& id);

    /// True iff @p id should be disabled (crash count >= threshold).
    bool shouldDisable(const std::string& id);

private:
    static constexpr int kCrashThreshold = 3;
    std::map<std::string, int> crash_counts_;
};

}  // namespace astra::recovery
