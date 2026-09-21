package eu.kanade.tachiyomi.data.track.anilist.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ALMangaTest {

    private val items = TrackerHarness.json
        .decodeFromString<ALSearchResult>(fixture("anilist", "search.json"))
        .data
        .page
        .media

    private val userItem = TrackerHarness.json
        .decodeFromString<ALUserListMangaQueryResult>(fixture("anilist", "user_list.json"))
        .data
        .page
        .mediaList
        .single()

    @Test
    fun searchTrackCarriesCredits() {
        val track = items[0].toALManga().toTrack()
        track.trackerId shouldBe TrackerManager.ANILIST
        track.remoteId shouldBe 101L
        track.title shouldBe "Solo Leveling"
        track.totalChapters shouldBe 179L
        track.coverUrl shouldBe "https://img/101.jpg"
        track.summary shouldBe "Hunters & gates\n"
        track.score shouldBe 85.0
        track.trackingUrl shouldBe "https://anilist.co/manga/101"
        track.publishingStatus shouldBe "FINISHED"
        track.publishingType shouldBe "Manhwa"
        track.startDate shouldBe "2018-03-04"
        track.authors shouldBe listOf("Preferred Name", "Writer Full")
        track.artists shouldBe listOf("Preferred Name", "画家")
    }

    @Test
    fun searchTrackWithoutOptionals() {
        val track = items[1].toALManga().toTrack()
        track.summary shouldBe ""
        track.startDate shouldBe ""
        track.authors shouldBe emptyList()
        track.artists shouldBe emptyList()
        formatStartDate(0L) shouldBe "1970-01-01"
    }

    @Test
    fun userMangaMapsToTrack() {
        val userManga = userItem.toALUserManga()
        exerciseDto(userManga)
        val track = userManga.toTrack()
        track.trackerId shouldBe TrackerManager.ANILIST
        track.remoteId shouldBe 101L
        track.title shouldBe "Solo Leveling"
        track.status shouldBe Anilist.READING
        track.score shouldBe 80.0
        track.startedReadingDate shouldBe ALFuzzyDate(2023, 5, 6).toEpochMilli()
        track.finishedReadingDate shouldBe 0L
        track.lastChapterRead shouldBe 12.0
        track.libraryId shouldBe 5001L
        track.totalChapters shouldBe 179L
        track.private shouldBe true
    }

    @Test
    fun userMangaStatusesMap() {
        val expected = mapOf(
            "CURRENT" to Anilist.READING,
            "COMPLETED" to Anilist.COMPLETED,
            "PAUSED" to Anilist.ON_HOLD,
            "DROPPED" to Anilist.DROPPED,
            "PLANNING" to Anilist.PLAN_TO_READ,
            "REPEATING" to Anilist.REREADING,
        )
        expected.forEach { (api, status) ->
            userItem.toALUserManga().copy(listStatus = api).toTrack().status shouldBe status
        }
        shouldThrow<IllegalArgumentException> { userItem.toALUserManga().copy(listStatus = "NOPE").toTrack() }
    }
}
