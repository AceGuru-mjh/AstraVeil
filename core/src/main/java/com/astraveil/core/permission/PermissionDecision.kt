package com.astraveil.core.permission

/**
 * v3 permission decision returned by [PermissionEngine.evaluate].
 *
 * Three-valued so the UI can prompt the user for the REQUIRE_APPROVAL
 * case rather than silently denying.
 */
enum class PermissionDecision {
    /** Permission is granted without prompting. */
    ALLOW,

    /** Permission is denied; no prompt. */
    DENY,

    /** The request needs an explicit user prompt before it can be granted. */
    REQUIRE_APPROVAL,
}
