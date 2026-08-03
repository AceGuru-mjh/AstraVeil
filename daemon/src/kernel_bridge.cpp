#include "kernel_bridge.h"

#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/mount.h>
#include <cstring>
#include <cerrno>

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "AstraDaemon.Kernel"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGI(...) printf(__VA_ARGS__)
#define LOGW(...) printf(__VA_ARGS__)
#define LOGE(...) printf(__VA_ARGS__)
#endif

namespace astra {

int KernelBridge::fd_ = -1;
bool KernelBridge::rooted_ = false;

bool KernelBridge::open() {
    if (fd_ >= 0) return true;

    fd_ = ::open(ASTRA_DEV_PATH, O_RDWR);
    if (fd_ < 0) {
        LOGE("Cannot open %s: %s", ASTRA_DEV_PATH, strerror(errno));
        return false;
    }

    LOGI("Opened %s (fd=%d)", ASTRA_DEV_PATH, fd_);
    return true;
}

void KernelBridge::close() {
    if (fd_ >= 0) {
        ::close(fd_);
        fd_ = -1;
        rooted_ = false;
    }
}

bool KernelBridge::is_available() {
    return access(ASTRA_DEV_PATH, F_OK) == 0;
}

bool KernelBridge::get_root() {
    if (rooted_) return true;
    if (!open()) return false;

    int ret = ioctl(fd_, ASTRA_IOCTL_GET_ROOT);
    if (ret != 0) {
        LOGE("GET_ROOT failed: %s (errno=%d)", strerror(errno), errno);
        return false;
    }

    if (getuid() != 0) {
        LOGE("GET_ROOT ioctl succeeded but uid != 0 (uid=%d)", getuid());
        return false;
    }

    rooted_ = true;
    LOGI("GET_ROOT success, uid=%d, gid=%d", getuid(), getgid());
    return true;
}

bool KernelBridge::get_capabilities(uint64_t caps, uint32_t uid) {
    if (!open()) return false;

    AstraCaps c{};
    c.cap_effective = caps;
    c.cap_permitted = caps;
    c.target_uid = uid;
    c.target_gid = uid;
    c.result = -1;

    int ret = ioctl(fd_, ASTRA_IOCTL_GET_CAPS, &c);
    if (ret != 0 || c.result != 0) {
        LOGE("GET_CAPS failed: ret=%d, result=%d, caps=0x%llx",
             ret, c.result, (unsigned long long)caps);
        return false;
    }

    LOGI("GET_CAPS success, caps=0x%llx, uid=%d",
         (unsigned long long)caps, uid);
    return true;
}

bool KernelBridge::load_policy(uint32_t uid, uint64_t caps, bool full_root) {
    if (!open()) return false;

    AstraPolicyEntry e{};
    e.uid = uid;
    e.allowed_caps = caps;
    e.allow_full_root = full_root ? 1 : 0;
    e._pad = 0;

    int ret = ioctl(fd_, ASTRA_IOCTL_SET_POLICY, &e);
    if (ret != 0) {
        LOGE("SET_POLICY failed for uid %d: %s", uid, strerror(errno));
        return false;
    }

    LOGI("Policy loaded: uid=%d, caps=0x%llx, full_root=%d",
         uid, (unsigned long long)caps, full_root);
    return true;
}

bool KernelBridge::mount(const std::string& source, const std::string& target,
                         const std::string& fstype, uint64_t flags,
                         const std::string& data) {
    if (!open()) return false;

    AstraMountReq req{};
    strncpy(req.source, source.c_str(), sizeof(req.source) - 1);
    strncpy(req.target, target.c_str(), sizeof(req.target) - 1);
    strncpy(req.fstype, fstype.c_str(), sizeof(req.fstype) - 1);
    req.flags = flags;
    if (!data.empty()) {
        strncpy(req.data, data.c_str(), sizeof(req.data) - 1);
    }
    req.result = -1;

    int ret = ioctl(fd_, ASTRA_IOCTL_MOUNT_HELPER, &req);
    if (ret != 0 || req.result != 0) {
        LOGE("MOUNT failed: %s -> %s (%s): ret=%d, result=%d",
             source.c_str(), target.c_str(), fstype.c_str(), ret, req.result);
        return false;
    }

    LOGI("MOUNT success: %s -> %s (%s)", source.c_str(), target.c_str(), fstype.c_str());
    return true;
}

bool KernelBridge::umount(const std::string& target, uint64_t flags) {
    // umount doesn't need kernel module assistance, call directly
    int ret = ::umount2(target.c_str(), (int)flags);
    if (ret != 0) {
        LOGE("UMOUNT failed: %s: %s", target.c_str(), strerror(errno));
        return false;
    }
    LOGI("UMOUNT success: %s", target.c_str());
    return true;
}

int KernelBridge::get_version() {
    if (!open()) return -1;

    int ver = 0;
    int ret = ioctl(fd_, ASTRA_IOCTL_GET_VERSION, &ver);
    if (ret != 0) {
        LOGE("GET_VERSION failed: %s", strerror(errno));
        return -1;
    }
    return ver;
}

bool KernelBridge::is_root() {
    return getuid() == 0;
}

} // namespace astra
