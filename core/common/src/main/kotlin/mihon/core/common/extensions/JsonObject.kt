package mihon.core.common.extensions

import kotlinx.serialization.json.JsonObject

private const val OPEN_BRACE: Byte = 0x7B
private const val CLOSE_BRACE: Byte = 0x7D

/** An empty JSON object. */
public val JsonObjectEmpty: JsonObject = JsonObject(emptyMap())

/** `{}` as UTF-8 bytes. */
public val JsonObjectEmptyBytes: ByteArray = byteArrayOf(OPEN_BRACE, CLOSE_BRACE)

/** An empty JSON object. */
public val JsonObject.Companion.EMPTY: JsonObject
    inline get() = JsonObjectEmpty
