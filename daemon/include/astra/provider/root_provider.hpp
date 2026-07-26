#pragma once

#include <string>

namespace astra::provider {

enum class RootType {
    NONE = 0,
    MAGISK = 1,
    KERNELSU = 2,
    APATCH = 3,
    ASTRA_ROOT = 4
};

class RootProvider {
public:
    virtual ~RootProvider() = default;

    virtual RootType type() const = 0;

    virtual bool available() const = 0;

    virtual bool execute(
        const std::string& command,
        std::string& output
    ) = 0;

    virtual std::string name() const = 0;
};

}  // namespace astra::provider
