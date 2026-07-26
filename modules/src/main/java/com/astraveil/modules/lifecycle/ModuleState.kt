package com.astraveil.modules.lifecycle

/**
 * v3 module state machine.
 *
 *   INSTALLED → RUNNING → STOPPED → (RUNNING again, or FAILED)
 *                     ↘ FAILED
 */
enum class ModuleState {
    INSTALLED,
    RUNNING,
    STOPPED,
    FAILED,
}
