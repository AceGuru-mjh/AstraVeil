package com.astraveil.modules.security

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class TrustedKey(val publicKeyB64: String, val fingerprint: String, val label: String, val trustedAt: Long)

class DeveloperKeyStore(context: Context) {
    private val file = File(context.filesDir, "trusted_developers.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val keys = mutableMapOf<String, TrustedKey>()

    init {
        if (file.exists()) {
            runCatching { json.decodeFromString<List<TrustedKey>>(file.readText()).forEach { keys[it.fingerprint] = it } }
        }
    }

    fun trustedKeySet(): Set<String> = keys.values.map { it.publicKeyB64 }.toSet()
    fun isTrusted(fingerprint: String): Boolean = keys.containsKey(fingerprint)
    fun trust(key: TrustedKey) { keys[key.fingerprint] = key; persist() }
    fun revoke(fingerprint: String) { keys.remove(fingerprint); persist() }
    fun all(): List<TrustedKey> = keys.values.toList()
    private fun persist() { file.writeText(json.encodeToString(keys.values.toList())) }
}
