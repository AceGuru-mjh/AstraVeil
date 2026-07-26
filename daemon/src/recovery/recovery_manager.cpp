#include "astra/recovery/recovery_manager.hpp"

#include "astra/logger/logger.hpp"

namespace astra::recovery {

void RecoveryManager::onDaemonCrash() {
    ALOGE("RecoveryManager: daemon crash detected — cleaning up");
    cleanup();
    restore();
}

void RecoveryManager::cleanup() {
    // Kill every running module process + destroy their sandboxes.
    // Phase 5.5: the ModuleRunner tracks live PIDs; cleanup iterates
    // and sends SIGTERM. For now we log the intent.
    ALOGI("RecoveryManager: cleanup — terminating module processes");
}

void RecoveryManager::restore() {
    ALOGI("RecoveryManager: restore — re-initialising daemon service");
    // Real restore re-runs DaemonService.start() + IpcServer.start().
}

}  // namespace astra::recovery
