package com.astraveil.providers

/**
 * Fallback [RootProvider] used when no root backend is detected.
 *
 * Ensures the app never crashes on an unrooted device — every call
 * returns a safe "not available" result.
 */
class NoneProvider : RootProvider {

    override val id: String = "none"
    override val displayName: String = "No Root"

    override suspend fun available(): Boolean = false

    override suspend fun detect(): RootInfo = RootInfo.none().copy(
        providerName = "none",
        displayName = "No Root",
    )

    @Suppress("DEPRECATION")
    override suspend fun execute(command: String): ProviderExecResult =
        ProviderExecResult(
            exitCode = -1,
            stdout = "",
            stderr = "No root provider available",
            success = false,
        )

    override suspend fun info(): RootInfo = RootInfo.none()

    override suspend fun mount(
        source: String,
        target: String,
        options: String
    ): Boolean = false

    override suspend fun capabilities(): Set<ProviderCapability> = emptySet()
}
