#pragma once
#include <map>
#include <string>

struct PropertyHook {
    static void install(const std::map<std::string, std::string> &props);
};
