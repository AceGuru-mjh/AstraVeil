// module-spoof/jni/include/config_reader.h
//
// 配置读取接口 — 从 /data/adb/astraveil/spoof/<pkg>.json
// 或 global.json 加载 SpoofConfig。

#pragma once
#include <string>
#include "spoof_config.h"

struct ConfigReader {
    static SpoofConfig load(const std::string &packageName);
};
