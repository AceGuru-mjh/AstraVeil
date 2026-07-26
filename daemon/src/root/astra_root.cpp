#include "astra/root/astra_root.hpp"

#include "astra/logger/logger.hpp"

namespace astra::root {

bool AstraRoot::initialize() {
    ALOGI("AstraRoot: initialize");
    ready_ = true;
    return true;
}

CommandResult AstraRoot::execute(const std::string& command) {
    if (!ready_) {
        CommandResult r;
        r.success = false;
        r.output = "AstraRoot not initialized";
        return r;
    }
    return command_.execute(command);
}

bool AstraRoot::mount(std::string source, std::string target) {
    return ready_ && mount_.mount(source, target);
}

bool AstraRoot::umount(std::string target) {
    return ready_ && mount_.umount(target);
}

bool AstraRoot::readFile(const std::string& path, std::string& out) {
    return ready_ && file_.read(path, out);
}

bool AstraRoot::writeFile(const std::string& path, const std::string& data) {
    return ready_ && file_.write(path, data);
}

}  // namespace astra::root
