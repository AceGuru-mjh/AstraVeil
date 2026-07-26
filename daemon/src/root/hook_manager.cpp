#include "astra/root/hook_manager.hpp"

#include "astra/logger/logger.hpp"

namespace astra::root {

bool HookManager::registerHook() {
    ALOGI("HookManager: registerHook (future: system service / property / framework)");
    return true;
}

bool HookManager::removeHook() {
    ALOGI("HookManager: removeHook");
    return true;
}

}  // namespace astra::root
