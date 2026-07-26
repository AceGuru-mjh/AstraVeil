#pragma once

#include "astra/module/module.hpp"

#include <string>

namespace astra::module {

/// Parses an AVM module's manifest.json into a [ModuleManifest].
///
/// Phase 6.1 skeleton: the actual JSON parsing is stubbed — [parse]
/// accepts the raw file contents (or a path, depending on caller) and
/// returns true once the fields are populated. A real JSON library
/// (e.g. nlohmann/json) lands in a later sub-phase; for now the parser
/// validates the non-empty preconditions so the rest of the module
/// pipeline (permission check → sandbox → runtime) can be wired end to
/// end.
class ManifestParser {
public:
    /// @param file  Raw manifest.json contents.
    /// @param output  Populated on success.
    /// @return true on success, false if the manifest is empty.
    static bool parse(
        const std::string& file,
        ModuleManifest& output
    );
};

}  // namespace astra::module
