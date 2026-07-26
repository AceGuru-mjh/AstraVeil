#include "astra/root/command_runtime.hpp"

#include <cstdio>
#include <string>

namespace astra::root {

CommandResult CommandRuntime::execute(const std::string& command) {
    CommandResult result;
    FILE* pipe = ::popen(command.c_str(), "r");
    if (!pipe) {
        result.success = false;
        return result;
    }
    char buffer[256];
    while (std::fgets(buffer, sizeof(buffer), pipe)) {
        result.output += buffer;
    }
    const int status = ::pclose(pipe);
    result.exit_code = status;
    result.success = (status == 0);
    return result;
}

}  // namespace astra::root
