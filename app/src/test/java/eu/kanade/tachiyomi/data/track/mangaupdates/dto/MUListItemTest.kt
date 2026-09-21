package eu.kanade.tachiyomi.data.track.mangaupdates.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MUListItemTest {

    private val json = TrackerHarness.json

    private inline fun <reified T> roundTrip(name: String): T {
        val decoded = json.decodeFromString<T>(fixture("mangaupdates", name))
        exerciseDto(decoded)
        json.decodeFromString<T>(json.encodeToString(decoded)) shouldBe decoded
        return decoded
    }

    @Test
    fun listItemCopiesStatusAndChapter() {
        val item = roundTrip<MUListItem>("list_item.json")
        item shouldBe MUListItem(MUSeries(87_654_321L, "Berserk"), 2L, MUStatus(3, 42), 1)
        val track = item.copyTo(dbTrack(TrackerManager.MANGAUPDATES))
        track.status shouldBe MangaUpdates.COMPLETE_LIST
        track.lastChapterRead shouldBe 42.0
    }

    @Test
    fun bareListItemDefaults() {
        val item = roundTrip<MUListItem>("list_item_bare.json")
        val track = item.copyTo(dbTrack(TrackerManager.MANGAUPDATES))
        track.status shouldBe MangaUpdates.READING_LIST
        track.lastChapterRead shouldBe 0.0
        MUListItem().copyTo(dbTrack(TrackerManager.MANGAUPDATES)).lastChapterRead shouldBe 0.0
        MUListItem(status = MUStatus()).status shouldBe MUStatus(null, null)
        MUSeries().id shouldBe null
    }

    @Test
    fun ratingCopiesScore() {
        roundTrip<MURating>("rating.json").copyTo(dbTrack(TrackerManager.MANGAUPDATES)).score shouldBe 8.5
        roundTrip<MURating>("rating_null.json").copyTo(dbTrack(TrackerManager.MANGAUPDATES)).score shouldBe 0.0
        MURating().rating shouldBe null
    }

    @Test
    fun loginAndProfileDecode() {
        roundTrip<MULoginResponse>("login.json") shouldBe MULoginResponse(MUContext("sess", 555L))
        roundTrip<MUCurrentUser>("profile.json") shouldBe MUCurrentUser("kuhy")
    }
}
