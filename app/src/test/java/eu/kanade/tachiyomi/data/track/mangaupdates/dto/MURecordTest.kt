package eu.kanade.tachiyomi.data.track.mangaupdates.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MURecordTest {

    private val json = TrackerHarness.json
    private val result = json.decodeFromString<MUSearchResult>(fixture("mangaupdates", "search.json"))
    private val records = result.results.map { it.record }

    @Test
    fun fullRecordMapsEveryField() {
        val track = records[0].toTrackSearch(TrackerManager.MANGAUPDATES)
        track.trackerId shouldBe TrackerManager.MANGAUPDATES
        track.remoteId shouldBe 87_654_321L
        track.title shouldBe "Berserk & Co"
        track.totalChapters shouldBe 0L
        track.coverUrl shouldBe "https://img/o.jpg"
        track.summary shouldBe "Guts & Griffith"
        track.trackingUrl shouldBe "https://www.mangaupdates.com/series/abc123/berserk"
        track.publishingStatus shouldBe ""
        track.publishingType shouldBe "Manga"
        track.startDate shouldBe "1989"
    }

    @Test
    fun emptyRecordFallsBack() {
        val track = records[1].toTrackSearch(TrackerManager.MANGAUPDATES)
        track.remoteId shouldBe 0L
        track.title shouldBe ""
        track.coverUrl shouldBe ""
        track.summary shouldBe ""
        track.trackingUrl shouldBe ""
        track.publishingType shouldBe "null"
        track.startDate shouldBe "null"
        records[2].toTrackSearch(TrackerManager.MANGAUPDATES).coverUrl shouldBe ""
        MURecord().toTrackSearch(TrackerManager.MANGAUPDATES).coverUrl shouldBe ""
    }

    @Test
    fun recordsRoundTrip() {
        exerciseDto(result)
        json.decodeFromString<MUSearchResult>(json.encodeToString(result)) shouldBe result
        records[0].authors shouldBe listOf(
            MUAuthor("Author", "Kentarou Miura"),
            MUAuthor("Artist", "Kentarou Miura"),
            MUAuthor("Author", null),
            MUAuthor("Artist", null),
        )
        records[0].image shouldBe MUImage(MUUrl("https://img/o.jpg", "https://img/t.jpg"), 10, 20)
        MUImage().url shouldBe null
        MUUrl().thumb shouldBe null
        MUAuthor().name shouldBe null
    }
}
