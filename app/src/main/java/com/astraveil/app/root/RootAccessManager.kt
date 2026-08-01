package com.astraveil.app.root

import com.astraveil.providers.RootProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class RootAccessStatus {
    GRANTED,
    DENIED,
    NO_BACKEND,
    ERROR,
}

/**
 * Bridges AstraVeil to a root backend's NATIVE grant mechanism.
 *
 * AstraVeil never exploits or patches anything itself. To obtain root it
 * asks the present backend through the standard su interface. That makes
 * the backend (Magisk / KernelSU / APatch) show its OWN superuser dialog
 * and record a per-app policy for AstraVeil.
 */
@Suppress("DEPRECATION")
object RootAccessManager {

    suspend fun requestAccess(provider: RootProvider): RootAccessStatus =
        withContext(Dispatchers.IO) {
            val available = runCatching { provider.available() }.getOrDefault(false)
            if (!available) return@withContext RootAccessStatus.NO_BACKEND

            val result = runCatching { provider.execute("id") }.getOrNull()
                ?: return@withContext RootAccessStatus.ERROR

            when {
                result.success && result.stdout.contains("uid=0") ->
                    RootAccessStatus.GRANTED
                else ->
                    RootAccessStatus.DENIED
            }
        }
}
