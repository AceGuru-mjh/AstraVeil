// module-spoof/jni/include/property_hook.h
//
// __system_property_get / __system_property_read_callback
// 拦截器接口。Dobby inline hook 安装点。

#pragma once
#include <map>
#include <string>

struct PropertyHook {
    static void install(const std::map<std::string, std::string> &props);
};
