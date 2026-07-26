#include "astra/root/root_runtime.hpp"

#include "astra/root/namespace_manager.hpp"
#include "astra/root/overlay_manager.hpp"
#include "astra/root/boot_manager.hpp"
#include "astra/logger/logger.hpp"

namespace astra::root {

RootRuntime::RootRuntime() = default;
RootRuntime::~RootRuntime() {
    stop();
}

bool RootRuntime::prepare() {
    if (prepared_) {
        return true;
    }
    /*
     * 1. 检测 kernel capability
     *    (CLONE_NEWNS / overlay filesystem / SELinux node)
     *    — handled by the subsystems at start() time; here we just mark
     *      prepared so start() knows we have been invoked.
     */
    ALOGI("RootRuntime: prepare complete");
    prepared_ = true;
    return true;
}

bool RootRuntime::start() {
    if (initialized_) {
        return true;
    }
    if (!prepared_ && !prepare()) {
        return false;
    }

    ns_      = std::make_unique<NamespaceManager>();
    overlay_ = std::make_unique<OverlayManager>();
    boot_    = std::make_unique<BootManager>();

    /*
     * 2. 初始化 namespace
     * 3. 准备 overlay
     */
    if (!ns_->create()) {
        ALOGE("RootRuntime: namespace create failed");
        return false;
    }

    // Overlay mounts are best-effort: a device without overlayfs support
    // still gets a working namespace; modules just see the real /system.
    (void)overlay_->mount_partition("system");
    (void)overlay_->mount_partition("vendor");

    // Boot detection is informational at start(); patching is explicit.
    (void)boot_->detect();

    initialized_ = true;
    ALOGI("RootRuntime: started (ready)");
    return true;
}

bool RootRuntime::stop() {
    if (!initialized_) {
        return true;
    }
    if (overlay_) {
        (void)overlay_->unmount("/mnt/astra/system");
        (void)overlay_->unmount("/mnt/astra/vendor");
        overlay_.reset();
    }
    if (ns_) {
        (void)ns_->destroy();
        ns_.reset();
    }
    boot_.reset();
    initialized_ = false;
    prepared_ = false;
    ALOGI("RootRuntime: stopped");
    return true;
}

bool RootRuntime::recover() {
    /*
     * Rollback to the snapshot taken at start() time. Used by the crash
     * guard when a module faults. Phase 8: tear down + restart.
     */
    ALOGI("RootRuntime: recover (restart)");
    stop();
    return start();
}

bool RootRuntime::ready() const {
    return initialized_;
}

NamespaceManager* RootRuntime::namespace_manager() {
    return ns_.get();
}

OverlayManager* RootRuntime::overlay_manager() {
    return overlay_.get();
}

BootManager* RootRuntime::boot_manager() {
    return boot_.get();
}

}  // namespace astra::root
