#include "astra/root/overlay_manager.hpp"

#ifdef __linux__
#include <sys/mount.h>
#endif

#include <filesystem>

#include "astra/logger/logger.hpp"

namespace astra::root {

namespace {

/// Build the mount option string for overlayfs:
///   lowerdir=...,upperdir=...,workdir=...
std::string build_options(const OverlayConfig& cfg) {
    std::string o;
    o.reserve(128);
    o += "lowerdir=";  o += cfg.lowerdir;
    o += ",upperdir="; o += cfg.upperdir;
    o += ",workdir=";  o += cfg.workdir;
    return o;
}

/// Ensure a directory exists (creating parents as needed).
bool ensure_dir(const std::string& path) {
    std::error_code ec;
    std::filesystem::create_directories(path, ec);
    return !ec;
}

}  // namespace

OverlayManager::OverlayManager() = default;
OverlayManager::~OverlayManager() = default;

bool OverlayManager::mount(const OverlayConfig& cfg) {
    /*
     * overlay:
     *   lowerdir = 原系统
     *   upperdir = Astra 修改层
     *   merged   = 目标
     */
    if (!ensure_dir(cfg.upperdir) || !ensure_dir(cfg.workdir) ||
        !ensure_dir(cfg.merged)) {
        ALOGE("OverlayManager: failed to create overlay dirs");
        return false;
    }

#ifdef __linux__
    const std::string opts = build_options(cfg);
    if (::mount("overlay", cfg.merged.c_str(), "overlay", 0,
                opts.c_str()) != 0) {
        ALOGE("OverlayManager: mount(%s) failed", cfg.merged.c_str());
        return false;
    }
    ALOGI("OverlayManager: mounted overlay at %s", cfg.merged.c_str());
    return true;
#else
    ALOGW("OverlayManager: overlay mount requires Linux");
    return false;
#endif
}

bool OverlayManager::mount_partition(const std::string& name) {
    OverlayConfig cfg;
    cfg.lowerdir = "/" + name;
    cfg.upperdir = "/data/astra/overlay/" + name + "/upper";
    cfg.workdir  = "/data/astra/overlay/" + name + "/work";
    cfg.merged   = "/mnt/astra/" + name;
    return mount(cfg);
}

bool OverlayManager::unmount(const std::string& target) {
#ifdef __linux__
    if (::umount(target.c_str()) != 0) {
        ALOGW("OverlayManager: umount(%s) failed", target.c_str());
        return false;
    }
    ALOGI("OverlayManager: unmounted %s", target.c_str());
    return true;
#else
    (void)target;
    return false;
#endif
}

}  // namespace astra::root
