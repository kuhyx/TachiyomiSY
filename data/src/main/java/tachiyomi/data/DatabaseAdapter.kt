package tachiyomi.data

import app.cash.sqldelight.ColumnAdapter
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.Date

/** Stores a [Date] as its epoch millis. */
public object DateColumnAdapter : ColumnAdapter<Date, Long> {
    override fun decode(databaseValue: Long): Date = Date(databaseValue)
    override fun encode(value: Date): Long = value.time
}

private const val LIST_OF_STRINGS_SEPARATOR = ", "

/** Stores a string list as one comma-separated string; an empty string reads back as an empty list. */
public object StringListColumnAdapter : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> = if (databaseValue.isEmpty()) {
        emptyList()
    } else {
        databaseValue.split(LIST_OF_STRINGS_SEPARATOR)
    }
    override fun encode(value: List<String>): String = value.joinToString(
        separator = LIST_OF_STRINGS_SEPARATOR,
    )
}

/** Stores an [UpdateStrategy] as its ordinal; an unknown ordinal reads back as [UpdateStrategy.ALWAYS_UPDATE]. */
public object UpdateStrategyColumnAdapter : ColumnAdapter<UpdateStrategy, Long> {
    override fun decode(databaseValue: Long): UpdateStrategy =
        UpdateStrategy.entries.getOrElse(databaseValue.toInt()) { UpdateStrategy.ALWAYS_UPDATE }

    override fun encode(value: UpdateStrategy): Long = value.ordinal.toLong()
}

/** Stores a [JsonObject] as UTF-8 JSON bytes. */
public object MemoColumnAdapter : ColumnAdapter<JsonObject, ByteArray> {
    override fun decode(databaseValue: ByteArray): JsonObject =
        Json.decodeFromString<JsonObject>(databaseValue.decodeToString())

    override fun encode(value: JsonObject): ByteArray = value.toString().encodeToByteArray()
}
