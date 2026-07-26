#pragma once

#include "astra/permission/permission_provider.hpp"

#include <memory>
#include <string>

namespace astra::service {

class PermissionService {
public:
    PermissionService();

    bool can_execute(
        const std::string& command
    ) const;

    std::string provider_name() const;

private:
    std::unique_ptr<
        permission::PermissionProvider
    > provider_;
};

}  // namespace astra::service
