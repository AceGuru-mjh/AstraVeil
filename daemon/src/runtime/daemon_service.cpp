#include "astra/runtime/daemon_service.hpp"

namespace astra {

DaemonService::DaemonService() : running_(false) {}

bool DaemonService::start() {
    running_ = true;
    return true;
}

void DaemonService::stop() {
    running_ = false;
}

bool DaemonService::isRunning() {
    return running_;
}

}  // namespace astra
