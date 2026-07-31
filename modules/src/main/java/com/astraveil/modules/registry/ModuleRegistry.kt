package com.astraveil.modules.registry

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ModuleRegistry(private val modulesRoot: File) {
    private val registryFile = File(modulesRoot, ".registry.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): MutableMap<String, ModuleRecord> {
        val persisted = readFromDisk()
        val reconciled = mutableMapOf<String, ModuleRecord>()
        for ((id, record) in persisted) {
            if (File(record.installPath).exists()) reconciled[id] = record
        }
        modulesRoot.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach { dir ->
            if (!reconciled.containsKey(dir.name)) {
                reconciled[dir.name] = ModuleRecord(
                    id = dir.name, installPath = dir.absolutePath, state = "INSTALLED",
                    sourceHash = null, signatureStatus = SignatureStatus.UNKNOWN,
                    installSource = "rebuild-from-disk", installTime = dir.lastModified(),
                )
            }
        }
        return reconciled
    }

    fun save(records: Collection<ModuleRecord>) {
        modulesRoot.mkdirs()
        val tmp = File(modulesRoot, ".registry.json.tmp")
        tmp.writeText(json.encodeToString(records.toList()))
        if (!tmp.renameTo(registryFile)) { tmp.copyTo(registryFile, overwrite = true); tmp.delete() }
    }

    private fun readFromDisk(): Map<String, ModuleRecord> {
        if (!registryFile.exists()) return emptyMap()
        return try {
            json.decodeFromString<List<ModuleRecord>>(registryFile.readText()).associateBy { it.id }
        } catch (e: Exception) {
            registryFile.renameTo(File(modulesRoot, ".registry.json.corrupt")); emptyMap()
        }
    }
}
