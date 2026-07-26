#include "astra/service/permission_service.hpp"

#include <unistd.h>

namespace astra::service {

class BasicPermissionProvider final
        : public permission::PermissionProvider {
public:
    permission::PermissionLevel level() const override {
        if (getuid() == 0) {
            return permission::PermissionLevel::ROOT;
        }

        return permission::PermissionLevel::NONE;
    }

    bool allow_execute(
        const std::string&
    ) const override {
        return false;
    }

    std::string name() const override {
        return "basic";
    }
};

PermissionService::PermissionService() {
    provider_ = std::make_unique<
        BasicPermissionProvider
    >();
}

bool PermissionService::can_execute(
    const std::string& command
) const {
    return provider_
        && provider_->allow_execute(command);
}

std::string PermissionService::provider_name() const {
    return provider_
        ? provider_->name()
        : "none";
}

}  // namespace astra::service
