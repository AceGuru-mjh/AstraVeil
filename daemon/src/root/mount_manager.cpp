#include "astra/root/mount_manager.hpp"

#ifdef __linux__
#include <sys/mount.h>
#endif

#include "astra/logger/logger.hpp"

namespace astra::root {

bool MountManager::mount(const std::string& source, const std::string& target) {
#ifdef __linux__
    if (::mount(source.c_str(), target.c_str(), nullptr, MS_BIND, nullptr) != 0) {
        ALOGE("MountManager: bind mount %s -> %s failed", source.c_str(), target.c_str());
        return false;
    }
    return true;
#else
    (void)source; (void)target;
    return false;
#endif
}

bool MountManager::umount(const std::string& target) {
#ifdef __linux__
    return ::umount(target.c_str()) == 0;
#else
    (void)target;
    return false;
#endif
}

}  // namespace astra::root
