#include <string>
#include <unistd.h>
#include <sys/stat.h>

namespace astra {

bool su_path_exists() {
    const char* paths[] = {
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/vendor/bin/su", "/system/sd/xbin/su", nullptr
    };
    for (int i = 0; paths[i]; ++i) {
        if (access(paths[i], F_OK) == 0) return true;
    }
    return false;
}

}  // namespace astra
