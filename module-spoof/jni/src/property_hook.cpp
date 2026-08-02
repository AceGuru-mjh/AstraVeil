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

static std::map<std::string, std::string> g_props;
static std::mutex g_mutex;

static int (*orig_prop_get)(const char *, char *) = nullptr;
static void (*orig_prop_read_cb)(
    const prop_info *,
    void (*)(void *, const char *, const char *, uint32_t),
    void *
) = nullptr;

static int hooked_prop_get(const char *name, char *value) {
    if (name) {
        std::lock_guard<std::mutex> lk(g_mutex);
        auto it = g_props.find(name);
        if (it != g_props.end()) {
            strcpy(value, it->second.c_str());
            return static_cast<int>(it->second.length());
        }
    }
    return orig_prop_get(name, value);
}

struct CbCtx {
    void (*user_cb)(void *, const char *, const char *, uint32_t);
    void *user_cookie;
};

static void trampoline(void *ctx, const char *name,
                        const char *value, uint32_t serial) {
    auto *c = static_cast<CbCtx *>(ctx);
    if (name) {
        std::lock_guard<std::mutex> lk(g_mutex);
        auto it = g_props.find(name);
        if (it != g_props.end()) {
            c->user_cb(c->user_cookie, name, it->second.c_str(), serial);
            return;
        }
    }
    c->user_cb(c->user_cookie, name, value, serial);
}

static void hooked_read_cb(
    const prop_info *pi,
    void (*cb)(void *, const char *, const char *, uint32_t),
    void *cookie
) {
    CbCtx ctx{cb, cookie};
    orig_prop_read_cb(pi, trampoline, &ctx);
}

void PropertyHook::install(const std::map<std::string, std::string> &props) {
    {
        std::lock_guard<std::mutex> lk(g_mutex);
        g_props = props;
    }

    void *a1 = dlsym(RTLD_DEFAULT, "__system_property_get");
    if (a1 && !orig_prop_get) {
        DobbyHook(a1, (dobby_dummy_func_t)hooked_prop_get,
                  (dobby_dummy_func_t *)&orig_prop_get);
        LOGI("Hook __system_property_get OK");
    }

    void *a2 = dlsym(RTLD_DEFAULT, "__system_property_read_callback");
    if (a2 && !orig_prop_read_cb) {
        DobbyHook(a2, (dobby_dummy_func_t)hooked_read_cb,
                  (dobby_dummy_func_t *)&orig_prop_read_cb);
        LOGI("Hook __system_property_read_callback OK");
    }
}
