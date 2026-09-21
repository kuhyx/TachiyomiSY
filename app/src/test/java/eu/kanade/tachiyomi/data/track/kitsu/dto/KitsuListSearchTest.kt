package eu.kanade.tachiyomi.data.track.kitsu.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.kitsu.KitsuDateHelper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class KitsuListSearchTest {

    private val json = TrackerHarness.json

    private val bareManga = KitsuListSearchItemIncluded(
        id = 9L,
        attributes = KitsuListSearchItemIncludedAttributes(
            canonicalTitle = "Bare",
            chapterCount = null,
            mangaType = null,
            posterImage = null,
            synopsis = null,
            startDate = null,
            status = "finished",
        ),
    )

    private fun decode(name: String) = json.decodeFromString<KitsuListSearchResult>(fixture("kitsu", name))

    private fun entry(
        status: String = "current",
        ratingTwenty: Int? = null,
        startedAt: String? = null,
    ) = KitsuListSearchItemData(
        id = 5L,
        attributes = KitsuListSearchItemDataAttributes(
            status = status,
            startedAt = startedAt,
            finishedAt = null,
            ratingTwenty = ratingTwenty,
            progress = 3,
            private = false,
        ),
    )

    @Test
    fun fullEntryMapsEveryField() {
        val result = decode("library_entries.json")
        exerciseDto(result)
        val track = result.firstToTrack()
        track.trackerId shouldBe TrackerManager.KITSU
        track.remoteId shouldBe 42L
        track.libraryId shouldBe 501L
        track.title shouldBe "One Piece"
        track.totalChapters shouldBe 1000L
        track.coverUrl shouldBe "https://img/op.jpg"
        track.summary shouldBe "Pirates."
        track.trackingUrl shouldBe "https://kitsu.app/manga/42"
        track.publishingStatus shouldBe "current"
        track.publishingType shouldBe "manga"
        track.startDate shouldBe "2020-01-02T03:04:05.000Z"
        track.startedReadingDate shouldBe KitsuDateHelper.parse("2020-01-02T03:04:05.000Z")
        track.finishedReadingDate shouldBe 0L
        track.status shouldBe Kitsu.READING
        track.score shouldBe 7.5
        track.lastChapterRead shouldBe 12.0
        track.private shouldBe true
    }

    @Test
    fun bareEntryFallsBackToDefaults() {
        val track = KitsuListSearchResult(listOf(entry()), listOf(bareManga)).firstToTrack()
        track.totalChapters shouldBe 0L
        track.coverUrl shouldBe ""
        track.summary shouldBe ""
        track.publishingType shouldBe ""
        track.startDate shouldBe ""
        track.score shouldBe 0.0
        track.private shouldBe false
    }

    @Test
    fun coverWithoutOriginalIsEmpty() {
        val cover = bareManga.copy(attributes = bareManga.attributes.copy(posterImage = KitsuSearchItemCover(null)))
        KitsuListSearchResult(listOf(entry()), listOf(cover)).firstToTrack().coverUrl shouldBe ""
    }

    @Test
    fun everyStatusMaps() {
        val expected = mapOf(
            "current" to Kitsu.READING,
            "completed" to Kitsu.COMPLETED,
            "on_hold" to Kitsu.ON_HOLD,
            "dropped" to Kitsu.DROPPED,
            "planned" to Kitsu.PLAN_TO_READ,
        )
        expected.forEach { (api, status) ->
            KitsuListSearchResult(listOf(entry(status = api)), listOf(bareManga)).firstToTrack().status shouldBe status
        }
    }

    @Test
    fun unknownStatusFails() {
        shouldThrow<IllegalStateException> {
            KitsuListSearchResult(listOf(entry(status = "nope")), listOf(bareManga)).firstToTrack()
        }
    }

    @Test
    fun missingUserDataIsRejected() {
        shouldThrow<IllegalArgumentException> { decode("library_entries_empty.json").firstToTrack() }
    }

    @Test
    fun missingMangaDataIsRejected() {
        shouldThrow<IllegalArgumentException> { decode("library_entries_no_included.json").firstToTrack() }
    }

    @Test
    fun includedDefaultsToEmpty() {
        json.decodeFromString<KitsuListSearchResult>("""{"data":[]}""") shouldBe KitsuListSearchResult(emptyList())
    }
}
