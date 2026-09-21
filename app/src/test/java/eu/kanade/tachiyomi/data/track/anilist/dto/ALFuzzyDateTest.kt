package eu.kanade.tachiyomi.data.track.anilist.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

internal class ALFuzzyDateTest {

    @Test
    fun completeDateIsStartOfDay() {
        val expected = LocalDate.of(2018, 3, 4).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        ALFuzzyDate(2018, 3, 4).toEpochMilli() shouldBe expected
    }

    @Test
    fun missingPartIsZero() {
        ALFuzzyDate(null, null, null).toEpochMilli() shouldBe 0L
        ALFuzzyDate(2018, null, 4).toEpochMilli() shouldBe 0L
    }

    @Test
    fun invalidDateIsZero() {
        ALFuzzyDate(2018, 13, 4).toEpochMilli() shouldBe 0L
    }

    @Test
    fun decodesNulls() {
        val decoded = TrackerHarness.json.decodeFromString<ALFuzzyDate>("""{"year":2020,"month":null,"day":null}""")
        decoded shouldBe ALFuzzyDate(2020, null, null)
        exerciseDto(decoded)
    }
}
