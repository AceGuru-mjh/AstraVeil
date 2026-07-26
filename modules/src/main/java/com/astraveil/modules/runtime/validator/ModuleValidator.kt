package com.astraveil.modules.runtime.validator

import com.astraveil.modules.api.ModuleManifest

/**
 * Validates an AVM module manifest before install.
 *
 * Checks:
 *  - apiVersion >= 2 (the v3 runtime rejects v1 modules)
 *  - permissions non-empty (a module that requests nothing is suspicious)
 *  - every permission has a non-blank capability + reason
 */
class ModuleValidator {

    fun validate(manifest: ModuleManifest): Boolean {
        if (manifest.apiVersion < 2) return false
        if (manifest.permissions.isEmpty()) return false
        return manifest.permissions.all {
            it.capability.isNotBlank() && it.reason.isNotBlank()
        }
    }
}
