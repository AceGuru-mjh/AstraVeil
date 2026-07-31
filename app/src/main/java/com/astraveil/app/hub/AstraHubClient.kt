package com.astraveil.app.hub

import com.astraveil.app.update.UpdateVerifier
import com.astraveil.core.logger.AstraLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for the AstraHub module repository.
 *
 * Security: every downloaded module's SHA-256 is verified against the
 * index BEFORE it is handed to the installer. The installer then runs
 * the full TrustGate (signature + trust chain). Defense in depth:
 *   index sha256  -> transport integrity
 *   ASTRAVEIL.SIG -> authorship + trust chain
 */
class AstraHubClient(
    private val indexUrl: String =
        "https://raw.githubusercontent.com/AceGuru-mjh/AstraVeil/main/astrahub/modules/index.json",
) {
    companion object {
        private const val TAG = "AstraHubClient"
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Fetch and parse the module index. */
    suspend fun fetchIndex(): AstraHubIndex = withContext(Dispatchers.IO) {
        val conn = URL(indexUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            val body = conn.inputStream.bufferedReader().readText()
            json.decodeFromString<AstraHubIndex>(body)
        } finally {
            conn.disconnect()
        }
    }

    /** Case-insensitive search over name/description/id. */
    fun search(index: AstraHubIndex, query: String): List<HubModule> {
        if (query.isBlank()) return index.modules
        val q = query.lowercase()
        return index.modules.filter {
            it.name.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.id.lowercase().contains(q)
        }
    }

    /**
     * Download a module and verify its SHA-256 against the index.
     * @return the verified file, or null if download/verification failed
     */
    suspend fun downloadVerified(
        module: HubModule,
        destDir: File,
    ): File? = withContext(Dispatchers.IO) {
        destDir.mkdirs()
        val dest = File(destDir, "${module.id}-${module.version}.avm")
        try {
            val conn = URL(module.downloadUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.inputStream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            conn.disconnect()

            // Transport integrity: index-published sha256
            val actual = UpdateVerifier.computeSha256(dest)
            if (actual == null || !actual.equals(module.sha256, ignoreCase = true)) {
                AstraLogger.e(TAG, "sha256 mismatch for ${module.id}: " +
                    "expected=${module.sha256}, actual=$actual")
                dest.delete()
                return@withContext null
            }
            dest
        } catch (e: Exception) {
            AstraLogger.e(TAG, "download failed for ${module.id}", e)
            dest.delete()
            null
        }
    }
}
