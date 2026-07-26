package com.astraveil.core.update

import kotlinx.serialization.Serializable

/**
 * Metadata for an available update.
 *
 * @property version   target version string (e.g. "0.1.1")
 * @property url       download URL for the full APK / patch
 * @property sha256    expected SHA-256 of the downloaded file
 * @property size      file size in bytes
 * @property mandatory if true, the user cannot skip the update
 */
@Serializable
data class UpdateInfo(
    val version: String = "",
    val url: String = "",
    val sha256: String = "",
    val size: Long = 0,
    val mandatory: Boolean = false,
)
