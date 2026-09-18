package tachiyomi.data

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import java.util.Date

internal class DatabaseAdapterTest {
    @Test
    fun dateRoundTripsAsEpochMillis() {
        DateColumnAdapter.encode(Date(1234L)) shouldBe 1234L
        DateColumnAdapter.decode(1234L) shouldBe Date(1234L)
    }

    @Test
    fun emptyStringIsEmptyList() {
        StringListColumnAdapter.decode("") shouldBe emptyList()
        StringListColumnAdapter.encode(emptyList()) shouldBe ""
    }

    @Test
    fun stringListSplitsOnComma() {
        StringListColumnAdapter.decode("action, drama") shouldBe listOf("action", "drama")
        StringListColumnAdapter.encode(listOf("action", "drama")) shouldBe "action, drama"
    }

    @Test
    fun updateStrategyByOrdinal() {
        UpdateStrategyColumnAdapter.encode(UpdateStrategy.ONLY_FETCH_ONCE) shouldBe 1L
        UpdateStrategyColumnAdapter.decode(1L) shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        UpdateStrategyColumnAdapter.decode(0L) shouldBe UpdateStrategy.ALWAYS_UPDATE
    }

    @Test
    fun unknownOrdinalIsAlwaysUpdate() {
        UpdateStrategyColumnAdapter.decode(-1L) shouldBe UpdateStrategy.ALWAYS_UPDATE
        UpdateStrategyColumnAdapter.decode(UpdateStrategy.entries.size.toLong()) shouldBe UpdateStrategy.ALWAYS_UPDATE
    }

    @Test
    fun memoRoundTripsAsJsonBytes() {
        val memo = JsonObject(mapOf("key" to JsonPrimitive("value")))
        val bytes = MemoColumnAdapter.encode(memo)
        bytes.decodeToString() shouldBe """{"key":"value"}"""
        MemoColumnAdapter.decode(bytes) shouldBe memo
        MemoColumnAdapter.decode("{}".encodeToByteArray()) shouldBe JsonObject(emptyMap())
    }
}
