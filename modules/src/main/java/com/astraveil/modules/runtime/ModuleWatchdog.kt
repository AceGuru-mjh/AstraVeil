package com.astraveil.modules.runtime

/**
 * Monitors running module processes and reports their health.
 */
class ModuleWatchdog {

    fun check(pid: Int, exitCode: Int): ModuleHealth {
        return if (exitCode == 0) {
            ModuleHealth.HEALTHY
        } else {
            ModuleHealth.CRASHED
        }
    }
}

enum class ModuleHealth {
    HEALTHY,
    CRASHED,
}
