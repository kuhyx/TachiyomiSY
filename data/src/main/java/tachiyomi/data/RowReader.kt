package tachiyomi.data

import app.cash.sqldelight.db.SqlCursor

/**
 * Reads a cursor's columns left to right, so the mapper of a hand-written
 * query lists its columns in select order instead of naming indices.
 */
internal class RowReader(private val cursor: SqlCursor) {
    private var index = 0

    /** The next column as a non-null long. */
    fun long(): Long = cursor.getLong(index++)!!

    /** The next column as a nullable long. */
    fun longOrNull(): Long? = cursor.getLong(index++)

    /** The next column as a non-null string. */
    fun string(): String = cursor.getString(index++)!!

    /** The next column as a nullable string. */
    fun stringOrNull(): String? = cursor.getString(index++)

    /** The next column as a boolean stored as 0/1. */
    fun boolean(): Boolean = long() == 1L

    /** The next column as a non-null double. */
    fun double(): Double = cursor.getDouble(index++)!!

    /** The next column as a non-null blob. */
    fun bytes(): ByteArray = cursor.getBytes(index++)!!

    /** Steps over a column the mapper does not use and yields the null it maps to. */
    fun <T> skipped(): T? {
        index++
        return null
    }
}
