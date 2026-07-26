#include "astra/root/overlay_config.hpp"

namespace astra::root {

std::string build_overlay_options(const OverlayConfig& cfg) {
    std::string o;
    o.reserve(160);
    o += "lowerdir=";  o += cfg.lowerdir;
    o += ",upperdir="; o += cfg.upperdir;
    o += ",workdir=";  o += cfg.workdir;
    return o;
}

OverlayConfig build_overlay_config(const std::string& partition) {
    OverlayConfig cfg;
    cfg.lowerdir = "/" + partition;
    cfg.upperdir = "/data/astra/overlay/" + partition + "/upper";
    cfg.workdir  = "/data/astra/overlay/" + partition + "/work";
    cfg.merged   = "/mnt/astra/" + partition;
    return cfg;
}

}  // namespace astra::root
