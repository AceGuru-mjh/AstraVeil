package com.astraveil.core.update.github

/**
 * Compares semantic version strings.
 *
 * Handles formats like:
 *   "0.1.0", "v0.1.0", "0.1.0-alpha", "v0.1.0-alpha13", "1.2.3-beta1"
 *
 * Returns true if [remote] is newer than [current].
 */
object VersionComparator {

    fun isRemoteNewer(current: String, remote: String): Boolean {
        val cur = normalize(current)
        val rem = normalize(remote)
        if (cur == rem) return false

        val curParts = cur.split(".")
        val remParts = rem.split(".")

        val maxLen = maxOf(curParts.size, remParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrNull(i)?.toIntOrNull() ?: 0
            val r = remParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /** Strip "v" prefix and extract the numeric portion (e.g. "v0.1.0-alpha13" → "0.1.0"). */
    private fun normalize(version: String): String {
        var v = version.trim()
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1)
        // Cut at first non-numeric-dot character
        val sb = StringBuilder()
        for (c in v) {
            if (c.isDigit() || c == '.') sb.append(c) else break
        }
        return sb.toString().trimEnd('.')
    }
}
