package exh.md.handlers

import exh.md.dto.StatisticsMangaDto
import exh.md.dto.StatisticsMangaRatingDto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MangaDetailsOptionsTest {
    @Test
    fun preferencesCarryEverySetting() {
        val preferences = preferences(
            altTitlesInDesc = true,
            finalChapterInDesc = true,
            tryUsingFirstVolumeCover = true,
            preferExtensionLangTitle = false,
        )
        preferences.coverQuality shouldBe ".512.jpg"
        preferences.tryUsingFirstVolumeCover shouldBe true
        preferences.altTitlesInDesc shouldBe true
        preferences.finalChapterInDesc shouldBe true
        preferences.preferExtensionLangTitle shouldBe false
        preferences.copy(coverQuality = "") shouldBe preferences.copy(coverQuality = "")
        preferences.hashCode() shouldBe preferences.copy().hashCode()
        preferences.toString().contains("coverQuality") shouldBe true
    }

    @Test
    fun extrasHoldTheOptionalLookups() {
        val statistics = StatisticsMangaDto(StatisticsMangaRatingDto(average = 8.0, bayesian = 7.5))
        val extras = MangaDetailsExtras(listOf("1"), statistics, "cover.jpg")
        extras.simpleChapters shouldBe listOf("1")
        extras.statistics shouldBe statistics
        extras.coverFileName shouldBe "cover.jpg"
        val bare = extras.copy(statistics = null, coverFileName = null)
        bare.statistics.shouldBeNull()
        bare.coverFileName.shouldBeNull()
        bare.hashCode() shouldBe bare.copy().hashCode()
        extras.toString().contains("simpleChapters") shouldBe true
    }
}
