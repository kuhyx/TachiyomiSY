package eu.kanade.tachiyomi.data.track.myanimelist.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MALMangaTest {

    private val json = TrackerHarness.json

    private inline fun <reified T> roundTrip(name: String): T {
        val decoded = json.decodeFromString<T>(fixture("myanimelist", name))
        exerciseDto(decoded)
        json.decodeFromString<T>(json.encodeToString(decoded)) shouldBe decoded
        return decoded
    }

    @Test
    fun searchResultDecodesDefaults() {
        val result = roundTrip<MALSearchResult>("search.json")
        result.paging shouldBe MALSearchPaging(null)
        val bare = result.data[2].node
        bare.synopsis shouldBe ""
        bare.mean shouldBe -1.0
        bare.covers.shouldBeNull()
        bare.authors shouldBe emptyList()
        result.data[0].node.authors[0] shouldBe MALAuthorNode(MALAuthor(1868, "Kentarou", "Miura"), "Story & Art")
    }

    @Test
    fun mangaDetailsDecode() {
        val manga = roundTrip<MALManga>("manga_details.json")
        manga.covers shouldBe MALMangaCovers("https://img/2l.jpg", "https://img/2m.jpg")
        json.decodeFromString<MALMangaCovers>("""{"medium":"m"}""") shouldBe MALMangaCovers(medium = "m")
        val bare = MALManga(
            id = 4L,
            title = "Bare Manga",
            numChapters = 0L,
            covers = null,
            status = "finished",
            mediaType = "manhwa",
            startDate = null,
        )
        json.decodeFromString<MALSearchResult>(fixture("myanimelist", "search.json")).data[2].node shouldBe bare
    }

    @Test
    fun fullNameIsTrimmedOrNull() {
        MALAuthor(1, "Kentarou", "Miura").getFullName() shouldBe "Kentarou Miura"
        MALAuthor(1, "", "Miura").getFullName() shouldBe "Miura"
        MALAuthor(1, "", "").getFullName().shouldBeNull()
        MALAuthor(1, " ", "").getFullName().shouldBeNull()
    }

    @Test
    fun metadataAndListDtos() {
        roundTrip<MALMangaMetadata>("metadata.json").covers.large shouldBe ""
        roundTrip<MALMangaMetadata>("metadata_bare.json").synopsis.shouldBeNull()
        roundTrip<MALListItem>("list_item.json").myListStatus shouldBe MALListItemStatus(
            isRereading = false,
            status = "reading",
            numChaptersRead = 42.0,
            score = 8,
            startDate = "2024-01-02",
            finishDate = null,
        )
        roundTrip<MALListItem>("list_item_absent.json") shouldBe MALListItem(380L, null)
        roundTrip<MALUser>("user.json") shouldBe MALUser("kuhy")
    }
}
