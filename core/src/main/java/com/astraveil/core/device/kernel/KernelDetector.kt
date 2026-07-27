package com.astraveil.core.device.kernel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class KernelDetector {

    suspend fun detect(): KernelInfo = withContext(Dispatchers.IO) {
        KernelInfo(
            version = readKernelVersion(),
            architecture = System.getProperty("os.arch") ?: "unknown",
            overlayFs = checkFilesystem("overlay") || checkFilesystem("overlayfs"),
            ebpf = File("/sys/fs/bpf").exists(),
            landlock = checkLandlock(),
        )
    }

    private fun readKernelVersion(): String = try {
        File("/proc/version").bufferedReader().use { reader ->
            val line = reader.readLine() ?: return@use ""
            val marker = "Linux version "
            val start = line.indexOf(marker)
            if (start < 0) return@use line.trim()
            val rest = line.substring(start + marker.length)
            val firstSpace = rest.indexOf(' ')
            if (firstSpace < 0) rest.trim() else rest.substring(0, firstSpace).trim()
        }
    } catch (t: Throwable) { "" }

    private fun checkFilesystem(fsName: String): Boolean = try {
        File("/proc/filesystems").bufferedReader().useLines { lines ->
            lines.any { it.contains(fsName) }
        }
    } catch (t: Throwable) { false }

    private fun checkLandlock(): Boolean = try {
        // Landlock requires Linux 5.13+
        val ver = readKernelVersion()
        if (ver.isBlank()) return false
        val parts = ver.split(".")
        if (parts.size < 2) return false
        val major = parts[0].toIntOrNull() ?: return false
        val minor = parts[1].toIntOrNull() ?: return false
        major > 5 || (major == 5 && minor >= 13)
    } catch (t: Throwable) { false }
}
