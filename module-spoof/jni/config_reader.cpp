// module-spoof/jni/config_reader.cpp
//
// 配置读取优先级：
//   1. /data/adb/astraveil/spoof/<package>.json  ← per-app
//   2. /data/adb/astraveil/spoof/global.json     ← 全局
//
// 推理：per-app 优先是因为用户可能只想对特定应用伪装，
// 其他应用看到真实设备。全局配置是"一键换机"的兜底。

#include "config_reader.h"

#include <fstream>
#include <sys/stat.h>
#include "json.hpp"

#define LOG_TAG "AstraSpoof.Config"
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

using json = nlohmann::json;

static const char *CONFIG_DIR = "/data/adb/astraveil/spoof";

static bool fileExists(const std::string &path) {
    struct stat st;
    return stat(path.c_str(), &st) == 0;
}

static SpoofConfig parseConfig(const std::string &path) {
    SpoofConfig config;
    std::ifstream file(path);
    if (!file.good()) return config;

    try {
        json j = json::parse(file);
        config.enabled = j.value("enabled", false);
        config.profile_name = j.value("profile", "");

        if (j.contains("props") && j["props"].is_object()) {
            for (auto &[key, val] : j["props"].items()) {
                if (val.is_string()) {
                    config.props[key] = val.get<std::string>();
                }
            }
        }

        if (j.contains("gl") && j["gl"].is_object()) {
            config.gl_renderer = j["gl"].value("renderer", "");
            config.gl_vendor = j["gl"].value("vendor", "");
        }
    } catch (const json::exception &e) {
        LOGW("Config parse error [%s]: %s", path.c_str(), e.what());
    }

    return config;
}

SpoofConfig ConfigReader::load(const std::string &packageName) {
    // 1. per-app 配置
    std::string perAppPath =
        std::string(CONFIG_DIR) + "/" + packageName + ".json";
    if (fileExists(perAppPath)) {
        LOGI("Loading per-app config: %s", perAppPath.c_str());
        return parseConfig(perAppPath);
    }

    // 2. 全局配置
    std::string globalPath = std::string(CONFIG_DIR) + "/global.json";
    if (fileExists(globalPath)) {
        return parseConfig(globalPath);
    }

    // 3. 无配置
    return SpoofConfig{};
}
