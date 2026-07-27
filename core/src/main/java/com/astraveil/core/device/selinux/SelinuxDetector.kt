package com.astraveil.core.device.selinux

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SelinuxDetector {

    suspend fun detect(): SelinuxProfile = withContext(Dispatchers.IO) {
        val enforceFile = File("/sys/fs/selinux/enforce")
        val enforce = if (enforceFile.exists()) {
            try { enforceFile.bufferedReader().use { it.readLine()?.trim() == "1" } } catch (t: Throwable) { false }
        } else false

        SelinuxProfile(
            mode = if (enforce) "enforcing" else if (File("/sys/fs/selinux").exists()) "permissive" else "disabled",
            enforcing = enforce,
            policyVersion = readPolicyVersion(),
        )
    }

    private fun readPolicyVersion(): Int = try {
        File("/sys/fs/selinux/policyvers").bufferedReader().use { it.readLine()?.trim()?.toIntOrNull() ?: 0 }
    } catch (t: Throwable) { 0 }
}
