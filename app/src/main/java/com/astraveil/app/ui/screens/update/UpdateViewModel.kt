package com.astraveil.app.ui.screens.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.core.update.UpdateManager
import com.astraveil.core.update.UpdateState
import com.astraveil.core.version.Version
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val manager = UpdateManager()

    val state: StateFlow<UpdateState> = manager.state

    val currentVersion: String = Version.VERSION

    fun checkForUpdate() {
        viewModelScope.launch {
            manager.check()
            val s = state.value
            if (s is UpdateState.Available) {
                com.astraveil.app.notification.AstraNotificationManager.notifyUpdateAvailable(
                    context = getApplication(),
                    versionName = s.version,
                    releaseNotes = s.releaseNotes,
                )
            }
        }
    }

    fun downloadAndInstall() {
        viewModelScope.launch {
            val currentState = state.value
            if (currentState !is UpdateState.Available) return@launch

            val cacheDir = getApplication<Application>().cacheDir
            val file = manager.download(currentState.downloadUrl, cacheDir)
            if (file == null) return@launch

            val verified = manager.verify(file, "")
            if (!verified) return@launch

            // Trigger Android package installer via Intent
            installApk(file)
        }
    }

    private fun installApk(file: java.io.File) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(
                    androidx.core.content.FileProvider.getUriForFile(
                        getApplication(),
                        "${getApplication<Application>().packageName}.fileprovider",
                        file,
                    ),
                    "application/vnd.android.package-archive",
                )
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (t: Throwable) {
            // FileProvider not configured — fall back to direct path
        }
    }

    fun reset() {
        manager.reset()
    }
}
