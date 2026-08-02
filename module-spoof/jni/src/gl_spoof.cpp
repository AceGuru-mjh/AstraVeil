#include "gl_spoof.h"
#include <dlfcn.h>
#include <string.h>
#include <GLES2/gl2.h>
#include "dobby.h"

#define LOG_TAG "AstraSpoof.GL"
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::string g_renderer;
static std::string g_vendor;
static const GLubyte *(*orig_glGetString)(GLenum) = nullptr;
static void *(*orig_dlsym)(void *, const char *) = nullptr;

static const GLubyte *hooked_glGetString(GLenum name) {
    if (name == GL_RENDERER && !g_renderer.empty())
        return (const GLubyte *)g_renderer.c_str();
    if (name == GL_VENDOR && !g_vendor.empty())
        return (const GLubyte *)g_vendor.c_str();
    return orig_glGetString(name);
}

static void *hooked_dlsym(void *handle, const char *symbol) {
    void *r = orig_dlsym(handle, symbol);
    if (symbol && strcmp(symbol, "glGetString") == 0 && orig_glGetString)
        return (void *)hooked_glGetString;
    return r;
}

void GlSpoof::install(const std::string &renderer, const std::string &vendor) {
    g_renderer = renderer;
    g_vendor = vendor;

    void *a1 = dlsym(RTLD_DEFAULT, "glGetString");
    if (a1 && !orig_glGetString) {
        DobbyHook(a1, (dobby_dummy_func_t)hooked_glGetString,
                  (dobby_dummy_func_t *)&orig_glGetString);
        LOGI("Hook glGetString: %s / %s", renderer.c_str(), vendor.c_str());
    }

    void *a2 = dlsym(RTLD_DEFAULT, "dlsym");
    if (a2 && !orig_dlsym) {
        DobbyHook(a2, (dobby_dummy_func_t)hooked_dlsym,
                  (dobby_dummy_func_t *)&orig_dlsym);
    }
}
