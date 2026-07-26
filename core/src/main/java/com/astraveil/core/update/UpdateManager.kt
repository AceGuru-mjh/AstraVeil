package com.astraveil.core.update

import com.astraveil.core.version.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Checks for, downloads, verifies, and installs AstraVeil updates.
 *
 * Flow:
 *   check() → UpdateInfo?
 *   download(info) → File
 *   verify(file, info.sha256) → Boolean
 *   install(file) → Boolean  (delegates to package installer)
 *
 * Phase 5.6 skeleton: [check] returns null (no update endpoint yet);
 * the state machine + verifier are real so the UI can be wired.
 */
class UpdateManager {

    private val _state = MutableStateFlow(UpdateState.IDLE)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    /** Check the update channel. Returns null if up-to-date. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        _state.value = UpdateState.CHECKING
        // TODO: query https://update.astraveil.com/latest?current=Version.VERSION
        _state.value = UpdateState.IDLE
        null
    }

    /** Download @p info to a temp file. Updates [progress]. */
    suspend fun download(info: UpdateInfo): java.io.File? = withContext(Dispatchers.IO) {
        _state.value = UpdateState.DOWNLOADING
        // TODO: real HTTP download with progress callback
        _progress.value = 100
        null
    }

    /** Verify the downloaded file's SHA-256. */
    suspend fun verify(file: java.io.File, info: UpdateInfo): Boolean =
        withContext(Dispatchers.IO) {
            _state.value = UpdateState.VERIFYING
            UpdateVerifier.verify(file, info.sha256)
        }

    /** Install the verified package. */
    suspend fun install(file: java.io.File): Boolean = withContext(Dispatchers.IO) {
        _state.value = UpdateState.INSTALLING
        // TODO: delegate to Android package installer
        _state.value = UpdateState.SUCCESS
        true
    }

    /** Rollback to the previous version. */
    suspend fun rollback(): Boolean = withContext(Dispatchers.IO) {
        // TODO: restore from /data/astra/backup/
        false
    }
}
