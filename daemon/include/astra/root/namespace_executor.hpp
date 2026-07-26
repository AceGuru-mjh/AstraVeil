#pragma once

#include "astra/root/command_runtime.hpp"

namespace astra::root {

/// Runs commands inside the Astra namespace.
///
/// Flow:
/// @code
/// execute()
///   ↓
/// 检查 namespace
///   ↓
/// 进入 namespace
///   ↓
/// 执行 command
///   ↓
/// 返回结果
/// @endcode
class NamespaceExecutor {
public:
    CommandResult execute(const std::string& command);

private:
    bool enterNamespace();
};

}  // namespace astra::root
