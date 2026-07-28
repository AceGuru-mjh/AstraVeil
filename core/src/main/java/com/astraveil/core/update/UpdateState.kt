package com.astraveil.core.update

/**
 * v3 update state — sealed class with rich data for the UI.
 *
 * Replaces the old enum. The UI can `when`-match each state and render
 * the appropriate card (checking spinner, available card, progress bar,
 * error message, etc.).
 */
sealed class UpdateState {
    /** No check has been performed yet. */
    data object Idle : UpdateState()

    /** Currently querying GitHub Releases API. */
    data object Checking : UpdateState()

    /** An update is available. */
    data class Available(
        val version: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val apkSize: Long,
    ) : UpdateState()

    /** Downloading the APK. [progress] is 0–100. */
    data class Downloading(val progress: Int) : UpdateState()

    /** Verifying SHA-256 of the downloaded file. */
    data object Verifying : UpdateState()

    /** Installing the APK via Android PackageInstaller. */
    data object Installing : UpdateState()

    /** Update completed successfully. */
    data object Success : UpdateState()

    /** App is already on the latest version. */
    data object Latest : UpdateState()

    /** An error occurred. [message] describes what went wrong. */
    data class Error(val message: String) : UpdateState()
}
