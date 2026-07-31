package com.astraveil.tools.avmsign

import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object AvMSigner {
    private const val MF_PATH = "META-INF/ASTRAVEIL.MF"
    private const val SIG_PATH = "META-INF/ASTRAVEIL.SIG"
    private const val CERT_PATH = "META-INF/ASTRAVEIL.CERT"

    @JvmStatic
    fun main(args: Array<String>) {
        when (args.firstOrNull()) {
            "keygen" -> keygen(args.getOrNull(1) ?: "dev")
            "sign" -> sign(args.getOrNull(1) ?: error("usage: sign <module.avm> <priv.pem> <signer>"), args.getOrNull(2) ?: error("missing priv.pem"), args.getOrNull(3) ?: "unknown")
            else -> { System.err.println("commands: keygen <name> | sign <avm> <priv.pem> <signer>"); kotlin.system.exitProcess(1) }
        }
    }

    private fun keygen(name: String) {
        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        File("$name.priv.pem").writeText(Base64.getEncoder().encodeToString(kp.private.encoded))
        File("$name.pub.pem").writeText(Base64.getEncoder().encodeToString(kp.public.encoded))
        println("Wrote $name.priv.pem and $name.pub.pem")
    }

    private fun sign(avmPath: String, privPem: String, signer: String) {
        val src = File(avmPath); require(src.exists()) { "avm not found" }
        val privBytes = Base64.getDecoder().decode(File(privPem).readText().trim())
        val privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(PKCS8EncodedKeySpec(privBytes))
        val pubBytes = Base64.getDecoder().decode(File(privPem.replace(".priv.pem", ".pub.pem")).readText().trim())
        val entries = LinkedHashMap<String, ByteArray>()
        ZipFile(src).use { zip -> for (e in zip.entries().toList()) { if (!e.isDirectory && !e.name.startsWith("META-INF/ASTRAVEIL.")) entries[e.name] = zip.getInputStream(e).readBytes() } }
        val mf = buildManifest(signer, entries); val mfBytes = mf.toByteArray(Charsets.UTF_8)
        val sig = Signature.getInstance("Ed25519"); sig.initSign(privateKey); sig.update(mfBytes); val sigBytes = sig.sign()
        val out = File(src.nameWithoutExtension + ".signed.avm")
        ZipOutputStream(out.outputStream()).use { zos ->
            for ((path, content) in entries) { zos.putNextEntry(ZipEntry(path)); zos.write(content); zos.closeEntry() }
            fun put(path: String, bytes: ByteArray) { zos.putNextEntry(ZipEntry(path)); zos.write(bytes); zos.closeEntry() }
            put(MF_PATH, mfBytes); put(SIG_PATH, sigBytes); put(CERT_PATH, Base64.getEncoder().encode(pubBytes))
        }
        println("Signed -> ${out.absolutePath}")
    }

    private fun buildManifest(signer: String, entries: Map<String, ByteArray>): String {
        val sb = StringBuilder(); sb.append("Manifest-Version: 1.0\nSigner: $signer\nCreated: ${System.currentTimeMillis()}\n\n")
        val digest = MessageDigest.getInstance("SHA-256")
        for ((path, content) in entries.toSortedMap()) { sb.append("Name: $path\nSHA-256: ${digest.digest(content).joinToString("") { "%02x".format(it) }}\n\n") }
        return sb.toString()
    }
}
