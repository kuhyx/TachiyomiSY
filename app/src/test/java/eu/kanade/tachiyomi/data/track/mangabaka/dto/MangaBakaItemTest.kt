package eu.kanade.tachiyomi.data.track.mangabaka.dto

import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class MangaBakaItemTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun title(
        language: String,
        title: String,
        traits: List<String> = emptyList(),
        primary: Boolean = false,
    ): MangaBakaItemTitle = MangaBakaItemTitle(language = language, traits = traits, title = title, isPrimary = primary)

    private fun item(titles: List<MangaBakaItemTitle>?): MangaBakaItem = MangaBakaItem(
        id = 7L,
        cover = MangaBakaCover(MangaBakaScaledCover(null)),
        authors = null,
        artists = null,
        description = null,
        published = MangaBakaPublishData(null),
        status = "releasing",
        type = "manga",
        rating = null,
        titles = titles,
    )

    @Test
    fun decodesTheSeriesFixture() {
        val result = json.decodeFromString<MangaBakaItemResult>(fixture(FIXTURES + "series.json"))
        result.data.id shouldBe 1234L
        result.data.cover.x250.x1 shouldBe "https://cdn.mangabaka.org/1234/x250.jpg"
        result.data.published.startDate shouldBe "2019-04-01"
        result.data.rating shouldBe 8.456
        result.data.titles?.size shouldBe 3
        result.data.chooseBestTitle() shouldBe "Testing Story"
    }

    @Test
    fun decodesTheSearchFixture() {
        val result = json.decodeFromString<MangaBakaSearchResult>(fixture(FIXTURES + "search.json"))
        result.data.size shouldBe 2
        val bare = result.data[1]
        bare.cover.x250.x1 shouldBe null
        bare.authors shouldBe null
        bare.published.startDate shouldBe null
        bare.titles shouldBe null
        bare.chooseBestTitle() shouldBe "ID: 99 - Could not find name! (report on the MangaBaka Discord)"
    }

    @Test
    fun rankOrderWithinALanguage() {
        val titles = listOf(
            title("en", "other"),
            title("en", "native", traits = listOf("native")),
            title("en", "official", traits = listOf("official")),
            title("en", "primary", primary = true),
        )
        item(titles).chooseBestTitle() shouldBe "primary"
        item(titles.dropLast(1)).chooseBestTitle() shouldBe "official"
        item(titles.dropLast(2)).chooseBestTitle() shouldBe "native"
        item(titles.dropLast(3)).chooseBestTitle() shouldBe "other"
        // The first of several titles is ranked once more, outside the loop.
        item(titles.reversed()).chooseBestTitle() shouldBe "primary"
        item(listOf(titles[1], titles[0])).chooseBestTitle() shouldBe "native"
    }

    @Test
    fun languagePriorityIsOrdered() {
        item(listOf(title("ja", "ja"), title("ko-Latn", "ko-Latn"))).chooseBestTitle() shouldBe "ja"
        item(listOf(title("zh", "zh"), title("zh-Latn", "zh-Latn"))).chooseBestTitle() shouldBe "zh-Latn"
        item(listOf(title("ko", "ko"))).chooseBestTitle() shouldBe "ko"
    }

    @Test
    fun unknownLanguagesUseFirst() {
        item(listOf(title("fr", "premier"), title("de", "zweite"))).chooseBestTitle() shouldBe "premier"
        item(emptyList()).chooseBestTitle() shouldBe "ID: 7 - Could not find name! (report on the MangaBaka Discord)"
    }

    @Test
    fun dataClassesRoundTrip() {
        val original = item(listOf(title("en", "t", traits = listOf("official"), primary = true)))
        val decoded = json.decodeFromString<MangaBakaItem>(json.encodeToString(original))
        decoded shouldBe original
        decoded.copy(id = 8L).hashCode() shouldBe original.copy(id = 8L).hashCode()
        MangaBakaItemResult(original).toString() shouldBe "MangaBakaItemResult(data=$original)"
        MangaBakaSearchResult(listOf(original)).data.single() shouldBe original
    }

    private companion object {
        const val FIXTURES = "eu/kanade/tachiyomi/data/track/mangabaka/"
    }
}
