// module-spoof/jni/src/env_shield.h
//
// Environment Shield — unified detection-bypass engine.
//
// Intercepts file access (openat) to hide Magisk/Zygisk/Xposed/Frida
// traces, filters /proc/net/* for Frida ports and Magisk sockets,
// spoofs SELinux enforce state, and hooks syscall() for MOMO's
// direct-syscall detection.

#pragma once
#include <string>
#include <set>

struct ShieldConfig {
    bool hide_root = true;
    bool hide_magisk = true;
    bool hide_xposed = true;
    bool hide_mounts = true;
    bool hide_maps = true;
    bool hide_selinux = true;
    bool hide_debugger = true;
    bool hide_frida = true;
    bool hide_net_unix = true;
    // Per-tool bypass switches
    bool momo_bypass = false;
    bool ruru_bypass = false;
    bool chunqiu_bypass = false;
    bool hunter_bypass = false;
};

struct EnvShield {
    static void install(const ShieldConfig &config);
};
