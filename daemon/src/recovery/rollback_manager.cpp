#include "astra/recovery/rollback_manager.hpp"

#include "astra/logger/logger.hpp"

#include <filesystem>
#include <fstream>

namespace astra::recovery {

namespace {

std::string snapshot_path(const std::string& id) {
    return "/data/astra/recovery/" + id + ".snap";
}

}  // namespace

bool RollbackManager::createSnapshot(const std::string& id) {
    std::error_code ec;
    std::filesystem::create_directories(
        std::filesystem::path(snapshot_path(id)).parent_path(), ec);

    std::ofstream f(snapshot_path(id));
    if (!f.is_open()) {
        ALOGE("RollbackManager: cannot create snapshot %s", id.c_str());
        return false;
    }
    f << "astra-snapshot:" << id << "\n";
    ALOGI("RollbackManager: snapshot %s created", id.c_str());
    return true;
}

bool RollbackManager::restore(const std::string& id) {
    if (!std::filesystem::exists(snapshot_path(id))) {
        ALOGW("RollbackManager: snapshot %s not found", id.c_str());
        return false;
    }
    // TODO(Phase 9): swap overlay upperdir + reflash boot image.
    ALOGI("RollbackManager: restore %s (stub)", id.c_str());
    return true;
}

}  // namespace astra::recovery
