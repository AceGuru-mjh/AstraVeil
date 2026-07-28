package com.astraveil.core.update

import com.astraveil.core.update.github.GitHubUpdateChecker
import com.astraveil.core.update.github.VersionComparator
import com.astraveil.core.version.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * v3 UpdateManager — checks GitHub Releases, downloads APKs, verifies
 * SHA-256, and triggers Android's package installer.
 *
 * Flow:
 *   check() → UpdateState.Available?
 *   download() → File
 *   verify(file) → Boolean
 *   install(file) → launches ACTION_VIEW intent
 */
class UpdateManager(
    private val checker: GitHubUpdateChecker = GitHubUpdateChecker(),
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Check GitHub for a newer release. Updates [state]. */
    suspend fun check(): UpdateState = withContext(Dispatchers.IO) {
        _state.value = UpdateState.Checking
        try {
            val release = checker.fetchLatestRelease()
            if (release == null) {
                _state.value = UpdateState.Error("Failed to fetch release info")
                return@withContext _state.value
            }

            val remoteVersion = release.tagName
            if (!VersionComparator.isRemoteNewer(Version.VERSION, remoteVersion)) {
                _state.value = UpdateState.Latest
                return@withContext _state.value
            }

            val asset = checker.findApkAsset(release)
            if (asset == null) {
                _state.value = UpdateState.Error("No APK asset found in release $remoteVersion")
                return@withContext _state.value
            }

            _state.value = UpdateState.Available(
                version = remoteVersion,
                releaseNotes = release.body.take(500),
                downloadUrl = asset.downloadUrl,
                apkSize = asset.size,
            )
        } catch (t: Throwable) {
            _state.value = UpdateState.Error(t.message ?: "Unknown error")
        }
        _state.value
    }

    /** Download the APK to a temp file. Updates [state] with progress. */
    suspend fun download(url: String, destDir: File): File? = withContext(Dispatchers.IO) {
        try {
            _state.value = UpdateState.Downloading(0)
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
            }

            try {
                if (conn.responseCode != 200) {
                    _state.value = UpdateState.Error("Download failed: HTTP ${conn.responseCode}")
                    return@withContext null
                }

                val total = conn.contentLengthLong
                val file = File(destDir, "astraveil-update.apk")
                destDir.mkdirs()

                var downloaded = 0L
                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            downloaded += n
                            if (total > 0) {
                                val pct = (downloaded * 100 / total).toInt()
                                _state.value = UpdateState.Downloading(pct)
                            }
                        }
                    }
                }
                file
            } finally {
                conn.disconnect()
            }
        } catch (t: Throwable) {
            _state.value = UpdateState.Error("Download failed: ${t.message}")
            null
        }
    }

    /** Verify the downloaded file's SHA-256 (if hash is known). */
    suspend fun verify(file: File, expectedSha256: String): Boolean = withContext(Dispatchers.IO) {
        if (expectedSha256.isBlank()) return@withContext true // skip if no hash
        _state.value = UpdateState.Verifying
        UpdateVerifier.verify(file, expectedSha256)
    }

    /** Reset to idle state. */
    fun reset() {
        _state.value = UpdateState.Idle
    }
}
