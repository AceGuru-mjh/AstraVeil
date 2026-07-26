#pragma once

#include <string>

namespace astra::permission {

enum class PermissionLevel {
    NONE = 0,
    SHELL = 1,
    ROOT = 2
};

class PermissionProvider {
public:
    virtual ~PermissionProvider() = default;

    virtual PermissionLevel level() const = 0;

    virtual bool allow_execute(
        const std::string& command
    ) const = 0;

    virtual std::string name() const = 0;
};

}  // namespace astra::permission
