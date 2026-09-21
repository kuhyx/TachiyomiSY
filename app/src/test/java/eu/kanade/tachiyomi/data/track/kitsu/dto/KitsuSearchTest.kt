package eu.kanade.tachiyomi.data.track.kitsu.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class KitsuSearchTest {

    private val result = TrackerHarness.json
        .decodeFromString<KitsuAlgoliaSearchResult>(fixture("kitsu", "algolia_search.json"))
    private val hits = result.hits

    @Test
    fun fullHitMapsEveryField() {
        val track = hits[0].toTrack()
        track.trackerId shouldBe TrackerManager.KITSU
        track.remoteId shouldBe 1L
        track.title shouldBe "Manga One"
        track.totalChapters shouldBe 10L
        track.coverUrl shouldBe "https://img/1.jpg"
        track.summary shouldBe "Synopsis one"
        track.trackingUrl shouldBe "https://kitsu.app/manga/1"
        track.score shouldBe 81.5
        track.publishingStatus shouldBe "Finished"
        track.publishingType shouldBe "manga"
        track.startDate shouldBe "2020-01-01"
    }

    @Test
    fun missingFieldsUseDefaults() {
        val track = hits[1].toTrack()
        track.totalChapters shouldBe 0L
        track.coverUrl shouldBe ""
        track.summary shouldBe ""
        track.score shouldBe -1.0
        track.publishingStatus shouldBe "Publishing"
        track.publishingType shouldBe "novel"
        track.startDate shouldBe ""
    }

    @Test
    fun coverWithoutOriginalIsEmpty() {
        val track = hits[2].toTrack()
        track.coverUrl shouldBe ""
        track.publishingType shouldBe ""
    }

    @Test
    fun hitsDecodeAsDataClasses() {
        exerciseDto(result)
        hits[1] shouldBe KitsuAlgoliaSearchItem(
            id = 2L,
            canonicalTitle = "Novel Two",
            chapterCount = null,
            subtype = "novel",
            posterImage = null,
            synopsis = null,
            averageRating = null,
            startDate = null,
            endDate = null,
        )
    }
}
