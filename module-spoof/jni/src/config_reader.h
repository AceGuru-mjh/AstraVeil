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

struct ConfigReader {
    static SpoofConfig load(const std::string &packageName);
};
