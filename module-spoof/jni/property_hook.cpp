// module-spoof/jni/property_hook.cpp
//
// 拦截三个属性读取入口：
//   1. __system_property_get          ← 最常用，C 代码直接调用
//   2. __system_property_read_callback ← Android 8+ 新 API
//   3. __system_property_find + read   ← 部分应用先 find 再 read
//
// 推理：只 hook 入口 1 不够。Android 8+ 的 SystemProperties.get()
// 内部走 read_callback 路径。高级检测代码可能用 find+read 绕过。

#include "property_hook.h"

#include <dlfcn.h>
#include <string.h>
#include <sys/system_properties.h>
#include <map>
#include <string>
#include <mutex>

#include "dobby.h"

#define LOG_TAG "AstraSpoof.Prop"
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ── 伪装属性表（进程内全局，preAppSpecialize 时写入） ──
static std::map<std::string, std::string> g_spoofed_props;
static std::mutex g_props_mutex;

// ── 原始函数指针 ──
static int (*orig_prop_get)(const char *, char *) = nullptr;

static void (*orig_prop_read_callback)(
    const prop_info *,
    void (*)(void *, const char *, const char *, uint32_t),
    void *
) = nullptr;

// ════════════════════════════════════════════════════════
// Hook 1: __system_property_get
// ════════════════════════════════════════════════════════
static int hooked_prop_get(const char *name, char *value) {
    if (name) {
        std::lock_guard<std::mutex> lock(g_props_mutex);
        auto it = g_spoofed_props.find(name);
        if (it != g_spoofed_props.end()) {
            // 推理：strcpy 安全，因为 PROP_VALUE_MAX = 92，
            // 我们的伪装值都远小于此
            strcpy(value, it->second.c_str());
            return static_cast<int>(it->second.length());
        }
    }
    return orig_prop_get(name, value);
}

// ════════════════════════════════════════════════════════
// Hook 2: __system_property_read_callback
// ════════════════════════════════════════════════════════
// 推理：此函数同步调用 callback，因此栈上的 trampoline 上下文
// 在 callback 执行期间有效，无生命周期问题。

struct CallbackTrampoline {
    void (*user_callback)(void *, const char *, const char *, uint32_t);
    void *user_cookie;
};

static void trampoline_fn(void *ctx, const char *name,
                           const char *value, uint32_t serial) {
    auto *t = static_cast<CallbackTrampoline *>(ctx);
    if (name) {
        std::lock_guard<std::mutex> lock(g_props_mutex);
        auto it = g_spoofed_props.find(name);
        if (it != g_spoofed_props.end()) {
            t->user_callback(t->user_cookie, name,
                             it->second.c_str(), serial);
            return;
        }
    }
    t->user_callback(t->user_cookie, name, value, serial);
}

static void hooked_prop_read_callback(
    const prop_info *pi,
    void (*callback)(void *, const char *, const char *, uint32_t),
    void *cookie
) {
    CallbackTrampoline ctx{callback, cookie};
    orig_prop_read_callback(pi, trampoline_fn, &ctx);
}

// ════════════════════════════════════════════════════════
// 安装
// ════════════════════════════════════════════════════════
void PropertyHook::install(
    const std::map<std::string, std::string> &props
) {
    {
        std::lock_guard<std::mutex> lock(g_props_mutex);
        g_spoofed_props = props;
    }

    // Hook __system_property_get
    void *get_addr = dlsym(RTLD_DEFAULT, "__system_property_get");
    if (get_addr && !orig_prop_get) {
        int ret = DobbyHook(
            get_addr,
            reinterpret_cast<dobby_dummy_func_t>(hooked_prop_get),
            reinterpret_cast<dobby_dummy_func_t *>(&orig_prop_get)
        );
        LOGI("Hook __system_property_get: %s", ret == 0 ? "OK" : "FAIL");
    }

    // Hook __system_property_read_callback
    void *read_addr = dlsym(RTLD_DEFAULT, "__system_property_read_callback");
    if (read_addr && !orig_prop_read_callback) {
        int ret = DobbyHook(
            read_addr,
            reinterpret_cast<dobby_dummy_func_t>(hooked_prop_read_callback),
            reinterpret_cast<dobby_dummy_func_t *>(&orig_prop_read_callback)
        );
        LOGI("Hook __system_property_read_callback: %s",
             ret == 0 ? "OK" : "FAIL");
    }
}
