package eu.kanade.tachiyomi.data.track.kitsu.dto

import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class KitsuUserTest {

    private val json = TrackerHarness.json

    @Test
    fun decodesCurrentUser() {
        val result = json.decodeFromString<KitsuCurrentUserResult>(fixture("kitsu", "users.json"))
        result shouldBe KitsuCurrentUserResult(listOf(KitsuUser("9001", KitsuUserAttributes("kuhy"))))
        exerciseDto(result)
    }

    @Test
    fun decodesAddMangaResult() {
        val result = json.decodeFromString<KitsuAddMangaResult>(fixture("kitsu", "add_manga.json"))
        result shouldBe KitsuAddMangaResult(KitsuAddMangaItem(77L))
        exerciseDto(result)
        json.encodeToString(result) shouldBe """{"data":{"id":77}}"""
    }

    @Test
    fun decodesSearchKey() {
        val result = json.decodeFromString<KitsuSearchResult>(fixture("kitsu", "algolia_key.json"))
        result shouldBe KitsuSearchResult(KitsuSearchResultData("algolia-key"))
        exerciseDto(result)
    }

    @Test
    fun coverDecodesNullOriginal() {
        json.decodeFromString<KitsuSearchItemCover>("""{"original":null}""") shouldBe KitsuSearchItemCover(null)
        json.encodeToString(KitsuSearchItemCover("x")) shouldBe """{"original":"x"}"""
    }
}
