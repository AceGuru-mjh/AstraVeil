#pragma once
#include <string>

struct GlSpoof {
    static void install(const std::string &renderer, const std::string &vendor);
};
