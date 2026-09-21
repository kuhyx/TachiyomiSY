package eu.kanade.tachiyomi.data.track.bangumi.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class BGMSearchTest {

    private val json = TrackerHarness.json
    private val result = json.decodeFromString<BGMSearchResult>(fixture("bangumi", "search.json"))
    private val subjects = result.data

    @Test
    fun chineseTitleAddsOriginalName() {
        val track = subjects[0].toTrackSearch(TrackerManager.BANGUMI)
        track.trackerId shouldBe TrackerManager.BANGUMI
        track.remoteId shouldBe 1L
        track.title shouldBe "灌篮高手"
        track.coverUrl shouldBe "https://img/1.jpg"
        track.summary shouldBe "作品原名：Slam Dunk\nBasketball."
        track.score shouldBe 9.1
        track.trackingUrl shouldBe "https://bangumi.tv/subject/1"
        track.totalChapters shouldBe 276L
        track.startDate shouldBe "1990-10-01"
    }

    @Test
    fun blankChineseTitleUsesName() {
        val track = subjects[1].toTrackSearch(TrackerManager.BANGUMI)
        track.title shouldBe "No Platform"
        track.coverUrl shouldBe ""
        track.summary shouldBe ""
        track.score shouldBe -1.0
        track.totalChapters shouldBe 0L
        track.startDate shouldBe ""
        subjects[1].copy(summary = " trimmed ").toTrackSearch(TrackerManager.BANGUMI).summary shouldBe "trimmed"
    }

    @Test
    fun nullNestedFieldsFallBack() {
        val track = subjects[2].toTrackSearch(TrackerManager.BANGUMI)
        track.coverUrl shouldBe ""
        track.score shouldBe -1.0
        track.summary shouldBe "作品原名：Novel\nn"
        subjects[2].copy(summary = null).toTrackSearch(TrackerManager.BANGUMI).summary shouldBe "作品原名：Novel"
    }

    @Test
    fun resultRoundTrips() {
        exerciseDto(result)
        json.decodeFromString<BGMSearchResult>(json.encodeToString(result)) shouldBe result
        result.total shouldBe 3
        subjects[1].infobox shouldBe emptyList()
        subjects[0].infobox shouldBe listOf(Infobox.SingleValue("作者", "井上雄彦"))
        json.decodeFromString<BGMSearchResult>("""{"total":0,"limit":20,"offset":0}""").data shouldBe emptyList()
    }

    @Test
    fun subjectConstructorDefaults() {
        val subject = BGMSubject(
            id = 9L,
            nameCn = "",
            name = "n",
            summary = null,
            date = null,
            images = null,
            rating = null,
            platform = null,
        )
        subject.volumes shouldBe 0L
        subject.eps shouldBe 0L
        BGMSearchResult(total = 0, limit = 1, offset = 2).data shouldBe emptyList()
    }
}
