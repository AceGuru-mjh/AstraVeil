#pragma once

#include <string>

namespace astra::update {

class UpdateManager {
public:
    bool check();
    bool verify();
    bool install();
    bool rollback();
};

}  // namespace astra::update
