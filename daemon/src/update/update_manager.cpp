#include "astra/update/update_manager.hpp"

#include "astra/logger/logger.hpp"

namespace astra::update {

bool UpdateManager::check() {
    ALOGI("UpdateManager: check (future: query update channel)");
    return false;  // no update available in stub
}

bool UpdateManager::verify() {
    ALOGI("UpdateManager: verify (future: hash + signature check)");
    return true;
}

bool UpdateManager::install() {
    ALOGI("UpdateManager: install (future: staged A/B install)");
    return true;
}

bool UpdateManager::rollback() {
    ALOGI("UpdateManager: rollback to previous slot");
    return true;
}

}  // namespace astra::update
