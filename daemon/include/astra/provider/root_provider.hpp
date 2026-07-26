#pragma once

#include <string>

namespace astra::provider {

enum class RootType {
    NONE,
    MAGISK,
    KERNELSU,
    APATCH,
    ASTRA_ROOT
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
