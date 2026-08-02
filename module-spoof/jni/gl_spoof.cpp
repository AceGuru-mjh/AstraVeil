// module-spoof/jni/gl_spoof.cpp
//
// 拦截 glGetString(GL_RENDERER) 和 glGetString(GL_VENDOR)。
//
// 推理：GL_RENDERER 由 GPU 驱动返回，用户态不可修改。
// 但 glGetString 是一个普通导出函数，可以 inline hook。
// 这是唯一能在不修改内核的情况下伪造 GPU 信息的方法。
//
// 限制：如果应用通过 dlopen("libGLESv2.so") 后 dlsym 获取
// glGetString 地址，则绕过我们的 hook（因为 dlsym 返回的是
// 原始地址，不是 hook 后的 trampoline）。
// 对策：同时 hook dlsym，当 symbol == "glGetString" 时返回
// 我们的 hooked 版本。（见 install() 中的 dlsym hook）

#include "gl_spoof.h"

#include <dlfcn.h>
#include <string.h>
#include <string>
#include <GLES2/gl2.h>

#include "dobby.h"

#define LOG_TAG "AstraSpoof.GL"
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::string g_renderer;
static std::string g_vendor;

// ── 原始函数 ──
static const GLubyte *(*orig_glGetString)(GLenum) = nullptr;
static void *(*orig_dlsym)(void *, const char *) = nullptr;

// ── Hook: glGetString ──
static const GLubyte *hooked_glGetString(GLenum name) {
    if (name == GL_RENDERER && !g_renderer.empty()) {
        return reinterpret_cast<const GLubyte *>(g_renderer.c_str());
    }
    if (name == GL_VENDOR && !g_vendor.empty()) {
        return reinterpret_cast<const GLubyte *>(g_vendor.c_str());
    }
    return orig_glGetString(name);
}

// ── Hook: dlsym（防止应用绕过） ──
static void *hooked_dlsym(void *handle, const char *symbol) {
    void *result = orig_dlsym(handle, symbol);
    if (symbol && strcmp(symbol, "glGetString") == 0 && orig_glGetString) {
        // 返回我们的 hooked 版本
        return reinterpret_cast<void *>(hooked_glGetString);
    }
    return result;
}

void GlSpoof::install(const std::string &renderer,
                       const std::string &vendor) {
    g_renderer = renderer;
    g_vendor = vendor;

    // Hook glGetString
    void *gl_addr = dlsym(RTLD_DEFAULT, "glGetString");
    if (gl_addr && !orig_glGetString) {
        DobbyHook(
            gl_addr,
            reinterpret_cast<dobby_dummy_func_t>(hooked_glGetString),
            reinterpret_cast<dobby_dummy_func_t *>(&orig_glGetString)
        );
        LOGI("Hook glGetString: renderer=%s vendor=%s",
             renderer.c_str(), vendor.c_str());
    }

    // Hook dlsym（反绕过）
    void *dlsym_addr = dlsym(RTLD_DEFAULT, "dlsym");
    if (dlsym_addr && !orig_dlsym) {
        DobbyHook(
            dlsym_addr,
            reinterpret_cast<dobby_dummy_func_t>(hooked_dlsym),
            reinterpret_cast<dobby_dummy_func_t *>(&orig_dlsym)
        );
    }
}
