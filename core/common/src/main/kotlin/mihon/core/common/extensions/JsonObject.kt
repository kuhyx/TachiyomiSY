package mihon.core.common.extensions

import kotlinx.serialization.json.JsonObject

public val JsonObjectEmpty: JsonObject = JsonObject(emptyMap())

public val JsonObjectEmptyBytes: ByteArray = byteArrayOf(0x7B, 0x7D)

public val JsonObject.Companion.EMPTY: JsonObject
    inline get() = JsonObjectEmpty
