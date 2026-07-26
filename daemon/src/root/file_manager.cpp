#include "astra/root/file_manager.hpp"

#include <fstream>
#include <filesystem>

namespace astra::root {

bool FileManager::read(const std::string& path, std::string& output) {
    std::ifstream f(path, std::ios::binary);
    if (!f.is_open()) {
        return false;
    }
    output.assign(std::istreambuf_iterator<char>(f),
                  std::istreambuf_iterator<char>());
    return true;
}

bool FileManager::write(const std::string& path, const std::string& data) {
    // Writes go through the overlay upperdir (the Astra namespace mounts
    // /system as an overlay), so this never touches the real partition.
    std::ofstream f(path, std::ios::binary | std::ios::trunc);
    if (!f.is_open()) {
        return false;
    }
    f.write(data.data(), static_cast<std::streamsize>(data.size()));
    return f.good();
}

bool FileManager::remove(const std::string& path) {
    std::error_code ec;
    return std::filesystem::remove(path, ec);
}

}  // namespace astra::root
