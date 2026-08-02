#include "config_reader.h"
#include <fstream>
#include <sys/stat.h>
#include "json.hpp"

#define LOG_TAG "AstraSpoof.Config"
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

using json = nlohmann::json;
static const char *DIR = "/data/adb/astraveil/spoof";

static bool exists(const std::string &p) {
    struct stat st;
    return stat(p.c_str(), &st) == 0;
}

static SpoofConfig parse(const std::string &path) {
    SpoofConfig c;
    std::ifstream f(path);
    if (!f.good()) return c;
    try {
        json j = json::parse(f);
        c.enabled = j.value("enabled", false);
        c.profile_name = j.value("profile", "");
        if (j.contains("props") && j["props"].is_object())
            for (auto &[k, v] : j["props"].items())
                if (v.is_string()) c.props[k] = v.get<std::string>();
        if (j.contains("gl") && j["gl"].is_object()) {
            c.gl_renderer = j["gl"].value("renderer", "");
            c.gl_vendor = j["gl"].value("vendor", "");
        }
    } catch (const json::exception &e) {
        LOGW("Parse error [%s]: %s", path.c_str(), e.what());
    }
    return c;
}

SpoofConfig ConfigReader::load(const std::string &pkg) {
    std::string perApp = std::string(DIR) + "/" + pkg + ".json";
    if (exists(perApp)) {
        LOGI("Per-app config: %s", perApp.c_str());
        return parse(perApp);
    }
    std::string global = std::string(DIR) + "/global.json";
    if (exists(global)) return parse(global);
    return SpoofConfig{};
}
