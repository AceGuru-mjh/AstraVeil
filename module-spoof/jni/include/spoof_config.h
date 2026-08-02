// module-spoof/jni/include/spoof_config.h
//
// AstraVeil Spoof Engine — 伪装配置数据结构
//
// 推理：将 SpoofConfig 单独成头文件，便于 entry.cpp、
// property_hook.cpp、gl_spoof.cpp、config_reader.cpp 共享，
// 避免循环依赖。

#pragma once
#include <map>
#include <string>

struct SpoofConfig {
    bool enabled = false;
    std::string profile_name;
    std::map<std::string, std::string> props;
    std::string gl_renderer;
    std::string gl_vendor;
};
