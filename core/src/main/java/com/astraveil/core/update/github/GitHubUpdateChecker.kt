package com.astraveil.core.update.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks GitHub Releases API for the latest AstraVeil release.
 *
 * Uses plain HttpURLConnection (no OkHttp dependency needed).
 * API endpoint: https://api.github.com/repos/AceGuru-mjh/AstraVeil/releases/latest
 */
class GitHubUpdateChecker(
    private val repoOwner: String = "AceGuru-mjh",
    private val repoName: String = "AstraVeil",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 15_000
            }

            try {
                if (conn.responseCode != 200) return@runCatching null
                val body = conn.inputStream.bufferedReader().readText()
                json.decodeFromString(GitHubRelease.serializer(), body)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /**
     * Find the best APK asset from a release.
     * Prefers "app-release.apk", falls back to "app-debug.apk",
     * then any .apk file.
     */
    fun findApkAsset(release: GitHubRelease): GitHubAsset? {
        if (release.assets.isEmpty()) return null
        return release.assets.firstOrNull { it.name == "app-release.apk" }
            ?: release.assets.firstOrNull { it.name == "app-debug.apk" }
            ?: release.assets.firstOrNull { it.name.endsWith(".apk") }
    }
}
