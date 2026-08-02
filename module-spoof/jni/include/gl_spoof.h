// module-spoof/jni/include/gl_spoof.h
//
// glGetString(GL_RENDERER / GL_VENDOR) 拦截器接口。
// 同时 hook dlsym 以防止应用绕过。

#pragma once
#include <string>

struct GlSpoof {
    static void install(const std::string &renderer,
                         const std::string &vendor);
};
