#include <string>
#include <fstream>
#include <sys/utsname.h>

namespace astra {

std::string read_kernel_version() {
    struct utsname buf;
    if (uname(&buf) == 0) {
        return std::string(buf.release);
    }
    return "unknown";
}

bool check_overlayfs() {
    std::ifstream f("/proc/filesystems");
    std::string line;
    while (std::getline(f, line)) {
        if (line.find("overlay") != std::string::npos) return true;
    }
    return false;
}

bool check_namespace() {
    std::ifstream f("/proc/self/ns/mnt");
    return f.good();
}

}  // namespace astra
