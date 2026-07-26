#include "astra/module/manifest_parser.hpp"

namespace astra::module {

bool ManifestParser::parse(
    const std::string& file,
    ModuleManifest& output
) {
    /*
     * Phase 6.1:
     *
     * 接入 JSON 库 (nlohmann/json) 后在此读取:
     *   - api_version
     *   - id
     *   - name
     *   - version
     *   - author
     *   - permissions[]  →  ModulePermission{name}
     *
     * For now we only validate that the input is non-empty so the rest
     * of the module pipeline (permission check → sandbox → runtime)
     * can be exercised end-to-end with a hand-built manifest.
     */
    if (file.empty()) {
        return false;
    }

    // TODO(Phase 6.2): real JSON parse. Until then leave `output` at
    // its defaults; callers that need a populated manifest construct
    // one directly.
    (void)output;
    return true;
}

}  // namespace astra::module
