package eu.kanade.tachiyomi.data.track.anilist.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ALSearchItemTest {

    private val result = TrackerHarness.json.decodeFromString<ALSearchResult>(fixture("anilist", "search.json"))
    private val items = result.data.page.media

    private fun item(format: String = "MANGA", country: String? = null) = ALSearchItem(
        id = 1L,
        title = ALItemTitle("t"),
        coverImage = ItemCover("c"),
        description = null,
        format = format,
        status = null,
        startDate = ALFuzzyDate(null, null, null),
        chapters = null,
        averageScore = null,
        staff = ALStaff(emptyList()),
    ).let { if (country == null) it else it.copy(countryOfOrigin = country) }

    @Test
    fun fullItemMapsEveryField() {
        val manga = items[0].toALManga()
        manga shouldBe ALManga(
            remoteId = 101L,
            title = "Solo Leveling",
            imageUrl = "https://img/101.jpg",
            description = "Hunters &amp; gates<br>",
            format = "Manhwa",
            publishingStatus = "FINISHED",
            startDateFuzzy = ALFuzzyDate(2018, 3, 4).toEpochMilli(),
            totalChapters = 179L,
            averageScore = 85,
            staff = items[0].staff,
        )
    }

    @Test
    fun nullFieldsFallBack() {
        val manga = items[1].toALManga()
        manga.format shouldBe "ONE-SHOT"
        manga.publishingStatus shouldBe ""
        manga.startDateFuzzy shouldBe 0L
        manga.totalChapters shouldBe 0L
        manga.averageScore shouldBe -1
    }

    @Test
    fun mangaFormatDependsOnCountry() {
        item(country = "KR").toALManga().format shouldBe "Manhwa"
        item(country = "CN").toALManga().format shouldBe "Manhua"
        item(country = "TW").toALManga().format shouldBe "Manhua"
        item(country = "JP").toALManga().format shouldBe "Manga"
        item().toALManga().format shouldBe "Manga"
        item(format = "LIGHT_NOVEL").toALManga().format shouldBe "LIGHT-NOVEL"
    }

    @Test
    fun staffNamePrefersUserPreferred() {
        ALStaffName(userPreferred = "p", native = "n", full = "f")() shouldBe "p"
        ALStaffName(userPreferred = null, native = "n", full = "f")() shouldBe "f"
        ALStaffName(userPreferred = null, native = "n", full = null)() shouldBe "n"
        ALStaffName()().shouldBeNull()
    }

    @Test
    fun countryDefaultsToEmpty() {
        items[1].countryOfOrigin shouldBe ""
        exerciseDto(result)
    }

    @Test
    fun idSearchResultDecodes() {
        val decoded = TrackerHarness.json.decodeFromString<ALIdSearchResult>(fixture("anilist", "search_by_id.json"))
        decoded.data.media.id shouldBe 105L
        decoded.data.media.countryOfOrigin shouldBe "TW"
        exerciseDto(decoded)
    }
}
