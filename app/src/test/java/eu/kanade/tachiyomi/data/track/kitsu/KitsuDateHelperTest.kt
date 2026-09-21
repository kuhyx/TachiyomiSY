package eu.kanade.tachiyomi.data.track.kitsu

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.Test

internal class KitsuDateHelperTest {

    @Test
    fun zeroConvertsToNull() {
        KitsuDateHelper.convert(0L).shouldBeNull()
    }

    @Test
    fun convertFormatsAsIsoWithMillis() {
        val formatted = checkNotNull(KitsuDateHelper.convert(1_700_000_000_123L))
        formatted shouldEndWith ".123Z"
        formatted.length shouldBe "yyyy-MM-ddTHH:mm:ss.SSSZ".length
    }

    @Test
    fun nullParsesToZero() {
        KitsuDateHelper.parse(null) shouldBe 0L
    }

    @Test
    fun parseRoundTripsConvert() {
        val millis = 1_700_000_000_456L
        KitsuDateHelper.parse(KitsuDateHelper.convert(millis)) shouldBe millis
    }
}
