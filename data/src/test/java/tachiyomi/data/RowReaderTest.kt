package tachiyomi.data

import app.cash.sqldelight.db.SqlCursor
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class RowReaderTest {
    private val cursor: SqlCursor = mockk {
        every { getLong(0) } returns 7L
        every { getLong(1) } returns null
        every { getString(2) } returns "text"
        every { getString(3) } returns null
        every { getLong(4) } returns 1L
        every { getLong(5) } returns 0L
        every { getDouble(6) } returns 2.5
        every { getBytes(7) } returns byteArrayOf(1, 2)
        every { getLong(9) } returns 9L
    }

    @Test
    fun readsColumnsLeftToRight() {
        val row = RowReader(cursor)
        row.long() shouldBe 7L
        row.longOrNull() shouldBe null
        row.string() shouldBe "text"
        row.stringOrNull() shouldBe null
        row.boolean() shouldBe true
        row.boolean() shouldBe false
        row.double() shouldBe 2.5
        row.bytes() shouldBe byteArrayOf(1, 2)
        row.skipped<String>() shouldBe null
        row.long() shouldBe 9L
    }
}
