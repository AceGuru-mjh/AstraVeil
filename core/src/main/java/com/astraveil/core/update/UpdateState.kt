package com.astraveil.core.update

/**
 * Update lifecycle state shown in the UI.
 */
enum class UpdateState {
    IDLE,
    CHECKING,
    DOWNLOADING,
    VERIFYING,
    INSTALLING,
    SUCCESS,
    FAILED,
}
