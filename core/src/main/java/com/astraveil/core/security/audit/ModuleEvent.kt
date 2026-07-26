package com.astraveil.core.security.audit

/**
 * Module lifecycle events recorded in the audit log.
 */
enum class ModuleEvent {
    INSTALL,
    START,
    STOP,
    EXECUTE,
    DENIED,
}
