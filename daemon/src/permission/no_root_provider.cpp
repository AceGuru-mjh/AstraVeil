#include "astra/permission/permission_provider.hpp"

#include <unistd.h>

namespace astra::permission {

class NoRootProvider final
        : public PermissionProvider {
public:
    PermissionLevel level() const override {
        if (getuid() == 0) {
            return PermissionLevel::ROOT;
        }

        return PermissionLevel::NONE;
    }

    bool allow_execute(
        const std::string&
    ) const override {
        /*
         * Phase 1:
         *
         * 默认禁止权限提升
         *
         * 后续由:
         * MagiskProvider
         * KernelSUProvider
         * AstraRootProvider
         *
         * 接管
         */

        return false;
    }

    std::string name() const override {
        return "none";
    }
};

}  // namespace astra::permission
