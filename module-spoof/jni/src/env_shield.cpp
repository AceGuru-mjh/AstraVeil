// module-spoof/jni/src/env_shield.cpp
//
// Environment Shield: blocks all known detection vectors.
//
// Intercepted file paths:
//   /proc/self/mountinfo  -> filter Magisk overlay mount lines
//   /proc/mounts          -> same
//   /proc/self/status     -> TracerPid set to 0
//   /proc/net/unix        -> filter Magisk daemon socket
//   /proc/net/tcp         -> filter Frida port (27042 = 0x69A2)
//   /proc/net/tcp6        -> same
//   /sys/fs/selinux/enforce -> return "1"
//   /data/adb/*           -> return ENOENT
//   /sbin/su, /system/xbin/su, etc -> return ENOENT
//
// Reasoning: all detection tools ultimately read files via the openat
// syscall. Hooking at the openat layer is the most complete solution —
// whether detection code uses fopen, open, or C++ ifstream, it all
// goes through openat.

#include "env_shield.h"

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <sys/mman.h>
#include <string>
#include <vector>

#include "dobby.h"

#define LOG_TAG "AstraSpoof.Shield"
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static ShieldConfig g_config;
static int (*orig_openat)(int, const char *, int, ...) = nullptr;

// -- Hidden file/directory prefixes --
static const char *HIDDEN_PREFIXES[] = {
    // Magisk
    "/data/adb/magisk",
    "/data/adb/modules",
    "/data/adb/lspd",
    "/data/adb/lsposed",
    "/data/adb/astraveil",
    "/data/adb/post-fs-data.d",
    "/data/adb/service.d",
    // su binary
    "/sbin/su",
    "/system/bin/su",
    "/system/xbin/su",
    "/system/sbin/su",
    "/vendor/bin/su",
    "/odm/bin/su",
    "/data/local/su",
    "/data/local/bin/su",
    "/data/local/xbin/su",
    "/system/etc/.installed_su_daemon",
    // Xposed / LSPosed
    "/data/misc/riru",
    "/system/lib/libxposed",
    "/system/lib64/libxposed",
    "/data/data/de.robv.android.xposed",
    "/data/data/org.lsposed",
    "/data/data/org.meowcat.edxposed",
    // Frida
    "/data/local/tmp/frida",
    "/data/local/tmp/re.frida",
    "/tmp/frida",
    // Generic
    "/system/app/Superuser.apk",
    "/system/app/SuperSU",
    "/system/app/KingRoot",
};

// -- Magisk mount signatures (mountinfo filter) --
static const char *MOUNT_SIGNATURES[] = {
    "magisk",
    "worker",       // Magisk overlay worker
    "/data/adb",
    "tmpfs /system",
    "tmpfs /vendor",
    "tmpfs /product",
    "mirror",
};

// -- Magisk unix socket names --
static const char *UNIX_SOCKET_SIGNATURES[] = {
    "magisk",
    "zygisk",
    "lsposed",
    "riru",
    "astraveil",
};

// -- Create in-memory file with given content --
// Uses syscall(__NR_memfd_create) for broad API-level compatibility
// (memfd_create libc wrapper only exists from API 30).
static int memfd_with(const std::string &content) {
    int fd = (int)syscall(__NR_memfd_create, "shield", 1u /* MFD_CLOEXEC */);
    if (fd < 0) return -1;
    write(fd, content.c_str(), content.size());
    lseek(fd, 0, SEEK_SET);
    return fd;
}

// -- Filter mountinfo/mounts --
static std::string filter_mounts(const std::string &raw) {
    std::string out;
    out.reserve(raw.size());
    size_t pos = 0;
    while (pos < raw.size()) {
        size_t nl = raw.find('\n', pos);
        if (nl == std::string::npos) nl = raw.size();
        std::string line = raw.substr(pos, nl - pos + 1);

        bool hide = false;
        for (auto sig : MOUNT_SIGNATURES) {
            if (line.find(sig) != std::string::npos) {
                hide = true;
                break;
            }
        }
        if (!hide) out += line;
        pos = nl + 1;
    }
    return out;
}

// -- Filter /proc/self/status (TracerPid -> 0) --
static std::string filter_status(const std::string &raw) {
    std::string out;
    size_t pos = 0;
    while (pos < raw.size()) {
        size_t nl = raw.find('\n', pos);
        if (nl == std::string::npos) nl = raw.size();
        std::string line = raw.substr(pos, nl - pos + 1);

        if (line.find("TracerPid:") != std::string::npos) {
            out += "TracerPid:\t0\n";
        } else {
            out += line;
        }
        pos = nl + 1;
    }
    return out;
}

// -- Filter /proc/net/unix (hide Magisk socket) --
static std::string filter_unix(const std::string &raw) {
    std::string out;
    size_t pos = 0;
    while (pos < raw.size()) {
        size_t nl = raw.find('\n', pos);
        if (nl == std::string::npos) nl = raw.size();
        std::string line = raw.substr(pos, nl - pos + 1);

        bool hide = false;
        for (auto sig : UNIX_SOCKET_SIGNATURES) {
            if (line.find(sig) != std::string::npos) {
                hide = true;
                break;
            }
        }
        if (!hide) out += line;
        pos = nl + 1;
    }
    return out;
}

// -- Filter /proc/net/tcp (hide Frida port) --
// Frida default port 27042 = 0x69A2
static std::string filter_tcp(const std::string &raw) {
    std::string out;
    size_t pos = 0;
    while (pos < raw.size()) {
        size_t nl = raw.find('\n', pos);
        if (nl == std::string::npos) nl = raw.size();
        std::string line = raw.substr(pos, nl - pos + 1);

        // 0x69A2 = 27042 (Frida)
        // 0x69A3 = 27043 (Frida portal)
        bool hide = false;
        if (g_config.hide_frida) {
            if (line.find(":69A2") != std::string::npos ||
                line.find(":69A3") != std::string::npos) {
                hide = true;
            }
        }
        if (!hide) out += line;
        pos = nl + 1;
    }
    return out;
}

// -- Read real file content --
static std::string read_real(int dirfd, const char *path, int flags) {
    int fd = orig_openat(dirfd, path, flags);
    if (fd < 0) return "";
    char buf[65536];
    std::string content;
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf))) > 0) {
        content.append(buf, n);
    }
    close(fd);
    return content;
}

// -- Path hiding check --
static bool should_hide_path(const char *path) {
    for (auto prefix : HIDDEN_PREFIXES) {
        if (strncmp(path, prefix, strlen(prefix)) == 0) {
            return true;
        }
    }
    // /data/adb/ entire directory (MOMO/chunqiu scan it)
    if (g_config.hide_magisk &&
        strncmp(path, "/data/adb", 9) == 0) {
        return true;
    }
    return false;
}

// -- Hook: openat --
static int hooked_openat(int dirfd, const char *pathname, int flags, ...) {
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, int);
        va_end(args);
    }

    if (pathname) {
        // -- Path hiding --
        if (should_hide_path(pathname)) {
            errno = ENOENT;
            return -1;
        }

        // -- /proc/self/mountinfo + /proc/mounts --
        if (g_config.hide_mounts) {
            if (strcmp(pathname, "/proc/self/mountinfo") == 0 ||
                strcmp(pathname, "/proc/mounts") == 0 ||
                strcmp(pathname, "/proc/self/mounts") == 0) {
                std::string raw = read_real(dirfd, pathname, flags);
                if (!raw.empty()) {
                    return memfd_with(filter_mounts(raw));
                }
            }
        }

        // -- /proc/self/status (TracerPid) --
        if (g_config.hide_debugger) {
            if (strcmp(pathname, "/proc/self/status") == 0) {
                std::string raw = read_real(dirfd, pathname, flags);
                if (!raw.empty()) {
                    return memfd_with(filter_status(raw));
                }
            }
        }

        // -- /proc/net/unix (Magisk socket) --
        if (g_config.hide_net_unix) {
            if (strcmp(pathname, "/proc/net/unix") == 0) {
                std::string raw = read_real(dirfd, pathname, flags);
                if (!raw.empty()) {
                    return memfd_with(filter_unix(raw));
                }
            }
        }

        // -- /proc/net/tcp + tcp6 (Frida port) --
        if (g_config.hide_frida) {
            if (strcmp(pathname, "/proc/net/tcp") == 0 ||
                strcmp(pathname, "/proc/net/tcp6") == 0) {
                std::string raw = read_real(dirfd, pathname, flags);
                if (!raw.empty()) {
                    return memfd_with(filter_tcp(raw));
                }
            }
        }

        // -- SELinux enforce --
        if (g_config.hide_selinux) {
            if (strcmp(pathname, "/sys/fs/selinux/enforce") == 0) {
                return memfd_with("1");
            }
        }

        // -- /proc/self/maps (module traces) --
        if (g_config.hide_maps) {
            if (strcmp(pathname, "/proc/self/maps") == 0 ||
                strcmp(pathname, "/proc/self/smaps") == 0) {
                std::string raw = read_real(dirfd, pathname, flags);
                if (!raw.empty()) {
                    // Filter module + dobby + zygisk related lines
                    std::string out;
                    size_t pos = 0;
                    while (pos < raw.size()) {
                        size_t nl = raw.find('\n', pos);
                        if (nl == std::string::npos) nl = raw.size();
                        std::string line = raw.substr(pos, nl - pos + 1);
                        bool hide =
                            line.find("astraveil") != std::string::npos ||
                            line.find("dobby") != std::string::npos ||
                            line.find("zygisk") != std::string::npos ||
                            line.find("lsposed") != std::string::npos ||
                            line.find("riru") != std::string::npos ||
                            line.find("xposed") != std::string::npos ||
                            line.find("magisk") != std::string::npos ||
                            line.find("frida") != std::string::npos;
                        if (!hide) out += line;
                        pos = nl + 1;
                    }
                    return memfd_with(out);
                }
            }
        }
    }

    if (flags & O_CREAT)
        return orig_openat(dirfd, pathname, flags, mode);
    return orig_openat(dirfd, pathname, flags);
}

// -- MOMO bypass: direct syscall interception --
// Reasoning: MOMO uses svc #0 to issue openat syscalls directly,
// bypassing libc's openat wrapper. Our Dobby hook is at the libc
// layer and cannot intercept direct syscalls.
// Countermeasure: hook the syscall() function itself (libc wrapper).
// If MOMO uses inline asm svc #0, it cannot be intercepted in
// userspace — would need seccomp-bpf or a kernel module (out of scope).
static long (*orig_syscall)(long, ...) = nullptr;

static long hooked_syscall(long number, ...) {
    va_list args;
    va_start(args, number);

    // __NR_openat = 56 (arm64) / 322 (arm32)
    if (number == __NR_openat) {
        int dirfd = va_arg(args, int);
        const char *pathname = va_arg(args, const char *);
        int flags = va_arg(args, int);
        va_end(args);

        if (pathname && should_hide_path(pathname)) {
            errno = ENOENT;
            return -1;
        }
        return orig_syscall(number, dirfd, pathname, flags);
    }

    // Pass through other syscalls
    // Reasoning: syscalls take at most 6 arguments
    long a1 = va_arg(args, long);
    long a2 = va_arg(args, long);
    long a3 = va_arg(args, long);
    long a4 = va_arg(args, long);
    long a5 = va_arg(args, long);
    long a6 = va_arg(args, long);
    va_end(args);
    return orig_syscall(number, a1, a2, a3, a4, a5, a6);
}

void EnvShield::install(const ShieldConfig &config) {
    g_config = config;

    // Hook openat
    void *openat_addr = dlsym(RTLD_DEFAULT, "openat");
    if (openat_addr && !orig_openat) {
        DobbyHook(openat_addr,
                  (dobby_dummy_func_t)hooked_openat,
                  (dobby_dummy_func_t *)&orig_openat);
        LOGI("EnvShield: openat hook installed");
    }

    // Hook syscall (MOMO bypass)
    if (config.momo_bypass) {
        void *syscall_addr = dlsym(RTLD_DEFAULT, "syscall");
        if (syscall_addr && !orig_syscall) {
            DobbyHook(syscall_addr,
                      (dobby_dummy_func_t)hooked_syscall,
                      (dobby_dummy_func_t *)&orig_syscall);
            LOGI("EnvShield: syscall hook installed (MOMO bypass)");
        }
    }

    LOGI("EnvShield: root=%d magisk=%d xposed=%d mounts=%d maps=%d "
         "selinux=%d debug=%d frida=%d unix=%d "
         "momo=%d ruru=%d chunqiu=%d hunter=%d",
         config.hide_root, config.hide_magisk, config.hide_xposed,
         config.hide_mounts, config.hide_maps, config.hide_selinux,
         config.hide_debugger, config.hide_frida, config.hide_net_unix,
         config.momo_bypass, config.ruru_bypass,
         config.chunqiu_bypass, config.hunter_bypass);
}
