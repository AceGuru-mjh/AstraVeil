package com.astraveil.core.error

/**
 * Unified error hierarchy for all AstraVeil operations.
 * Every fallible API should return Result<T> or throw a subclass of AstraError.
 */
sealed class AstraError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    // ---- Capability ----
    data class CapabilityProbeFailed(
        val path: String,
        override val message: String = "Failed to probe capability at $path",
        override val cause: Throwable? = null,
    ) : AstraError(message, cause)

    // ---- Permission ----
    data class PermissionDenied(
        val moduleId: String,
        val capability: String,
        override val message: String = "Permission denied: $moduleId → $capability",
    ) : AstraError(message)

    data class DangerousPermissionBlocked(
        val moduleId: String,
        val capability: String,
        override val message: String = "Dangerous permission blocked: $capability (approval withheld)",
    ) : AstraError(message)

    // ---- Daemon / IPC ----
    data class DaemonNotConnected(
        override val message: String = "Daemon is not connected",
    ) : AstraError(message)

    data class DaemonRequestFailed(
        val requestType: Int,
        val errorCode: Int,
        override val message: String = "Daemon request 0x${requestType.toString(16)} failed with code $errorCode",
    ) : AstraError(message)

    data class DaemonTimeout(
        val requestType: Int,
        val timeoutMs: Long,
        override val message: String = "Daemon request timed out after ${timeoutMs}ms",
    ) : AstraError(message)

    // ---- Module ----
    data class ModuleNotFound(
        val moduleId: String,
        override val message: String = "Module not found: $moduleId",
    ) : AstraError(message)

    data class ModuleValidationFailed(
        val moduleId: String,
        val reason: String,
        override val message: String = "Module validation failed: $moduleId — $reason",
    ) : AstraError(message)

    data class ModuleSandboxViolation(
        val moduleId: String,
        val syscall: String,
        override val message: String = "Sandbox violation: $moduleId attempted $syscall",
    ) : AstraError(message)

    // ---- Provider ----
    data class ProviderUnavailable(
        val providerName: String,
        override val message: String = "Root provider unavailable: $providerName",
    ) : AstraError(message)

    data class ExecutionFailed(
        val command: String,
        val exitCode: Int,
        val stderr: String,
        override val message: String = "Execution failed (exit=$exitCode): $command",
    ) : AstraError(message)

    // ---- Security ----
    data class SignatureVerificationFailed(
        val moduleId: String,
        override val message: String = "Signature verification failed for $moduleId",
    ) : AstraError(message)

    data class PolicyDenied(
        val moduleId: String,
        val capability: String,
        val riskLevel: Int,
        override val message: String = "Rust policy denied: $moduleId/$capability (risk=$riskLevel)",
    ) : AstraError(message)

    // ---- Config ----
    data class ConfigLoadFailed(
        override val message: String = "Failed to load configuration",
        override val cause: Throwable? = null,
    ) : AstraError(message, cause)

    // ---- Update ----
    data class UpdateCheckFailed(
        override val message: String = "Update check failed",
        override val cause: Throwable? = null,
    ) : AstraError(message, cause)

    data class UpdateVerificationFailed(
        val expectedSha256: String,
        val actualSha256: String,
        override val message: String = "SHA-256 mismatch: expected=$expectedSha256 actual=$actualSha256",
    ) : AstraError(message)
}
