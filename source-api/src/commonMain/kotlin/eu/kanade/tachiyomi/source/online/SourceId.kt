package eu.kanade.tachiyomi.source.online

import java.security.MessageDigest

private const val ID_BYTES: Int = 8
private const val BITS_PER_BYTE: Int = 8
private const val BYTE_MASK: Long = 0xff

/**
 * The id every extension source derives from its name, language and version: the first eight
 * bytes of `MD5("<name lowercased>/<lang>/<versionId>")` with the sign bit cleared. The app uses it
 * to name extension sources it never loads (blocklists, tracker-backed sources).
 */
public fun sourceIdOf(name: String, lang: String, versionId: Int): Long {
    val key = "${name.lowercase()}/$lang/$versionId"
    val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
    return (0 until ID_BYTES)
        .map { bytes[it].toLong() and BYTE_MASK shl BITS_PER_BYTE * (ID_BYTES - 1 - it) }
        .reduce(Long::or) and Long.MAX_VALUE
}
