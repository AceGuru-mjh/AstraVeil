package com.astraveil.modules.runtime

/**
 * v3 module lifecycle state.
 *
 *   INSTALLED → VALIDATING → STARTING → RUNNING → STOPPED → (loop)
 *                                      ↘ FAILED
 */
enum class ModuleState {
    INSTALLED,
    VALIDATING,
    STARTING,
    RUNNING,
    STOPPED,
    FAILED,
}
