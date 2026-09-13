package eu.kanade.tachiyomi.util.lang

import java.security.MessageDigest

private const val HIGH_NIBBLE = 0xF0
private const val LOW_NIBBLE = 0x0F
private const val NIBBLE_BITS = 4

/** Hex-encoded SHA-256 and MD5 digests. */
public object Hash {

    private val chars = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f',
    )

    private val MD5 get() = MessageDigest.getInstance("MD5")

    private val SHA256 get() = MessageDigest.getInstance("SHA-256")

    /** SHA-256 of [bytes] as lower-case hex. */
    public fun sha256(bytes: ByteArray): String = encodeHex(SHA256.digest(bytes))

    /** SHA-256 of the UTF-8 bytes of [string] as lower-case hex. */
    public fun sha256(string: String): String = sha256(string.toByteArray())

    /** MD5 of [bytes] as lower-case hex. */
    public fun md5(bytes: ByteArray): String = encodeHex(MD5.digest(bytes))

    /** MD5 of the UTF-8 bytes of [string] as lower-case hex. */
    public fun md5(string: String): String = md5(string.toByteArray())

    private fun encodeHex(data: ByteArray): String {
        val l = data.size
        val out = CharArray(l shl 1)
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = chars[(HIGH_NIBBLE and data[i].toInt()).ushr(NIBBLE_BITS)]
            out[j++] = chars[LOW_NIBBLE and data[i].toInt()]
            i++
        }
        return String(out)
    }
}
