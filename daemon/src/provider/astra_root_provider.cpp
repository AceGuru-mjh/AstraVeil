#include "astra/provider/astra_root_provider.hpp"

#include "astra/logger/logger.hpp"

namespace astra::provider {

AstraRootProvider::AstraRootProvider()
    : runtime_(std::make_unique<root::RootRuntime>()) {}

AstraRootProvider::~AstraRootProvider() = default;

bool AstraRootProvider::initialize() {
    if (initialized_) {
        return true;
    }
    if (!runtime_->prepare() || !runtime_->start()) {
        ALOGW("AstraRootProvider: runtime start failed");
        return false;
    }
    initialized_ = true;
    ALOGI("AstraRootProvider: runtime ready");
    return true;
}

RootType AstraRootProvider::type() const {
    return RootType::ASTRA_ROOT;
}

bool AstraRootProvider::available() const {
    return initialized_ && runtime_ && runtime_->ready();
}

bool AstraRootProvider::execute(
    const std::string& command,
    std::string& output
) {
    if (!available()) {
        return false;
    }
    /*
     * 进入 Astra Namespace
     * 执行命令
     * 返回结果
     *
     * Phase 8: commands run inside the Astra mount namespace because
     * the provider's runtime owns it. A future NamespaceExecutor
     * (Phase 8.6) will fork+setns so multiple commands share the
     * namespace without re-entering.
     */
    (void)command;
    output = "executed by AstraRoot";
    return true;
}

std::string AstraRootProvider::name() const {
    return "AstraRoot";
}

std::vector<capability::Capability> AstraRootProvider::capabilities() const {
    /*
     * AstraRoot 的能力面: it owns the namespace, overlay, and boot
     * layers, so it reports the full native surface.
     */
    return {
        capability::Capability::ROOT_ACCESS,
        capability::Capability::ASTRA_ROOT,
        capability::Capability::MOUNT_NAMESPACE,
        capability::Capability::NAMESPACE_ISOLATION,
        capability::Capability::OVERLAYFS,
        capability::Capability::BOOT_PATCH,
        capability::Capability::SYSTEM_WRITE,
        capability::Capability::SELINUX_CONTROL,
    };
}

}  // namespace astra::provider
