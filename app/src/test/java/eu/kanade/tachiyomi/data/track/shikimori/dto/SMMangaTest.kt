package eu.kanade.tachiyomi.data.track.shikimori.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SMMangaTest {

    private val result = TrackerHarness.json.decodeFromString<SMSearchResult>(fixture("shikimori", "search.json"))
    private val mangas = result.data.mangas

    @Test
    fun fullMangaMapsEveryField() {
        val track = mangas[0].toTrack(TrackerManager.SHIKIMORI)
        track.trackerId shouldBe TrackerManager.SHIKIMORI
        track.remoteId shouldBe 2L
        track.title shouldBe "Berserk"
        track.totalChapters shouldBe 380L
        track.coverUrl shouldBe "https://img/2.jpg"
        track.summary shouldBe "Guts."
        track.score shouldBe 9.4
        track.trackingUrl shouldBe "https://shikimori.io/mangas/2-berserk"
        track.publishingStatus shouldBe "ongoing"
        track.publishingType shouldBe "manga"
        track.startDate shouldBe "1989-08-25"
        track.authors shouldBe listOf("Kentarou Miura", "Only Writer")
        track.artists shouldBe listOf("Kentarou Miura", "Only Artist")
    }

    @Test
    fun zeroScoreAndNullDateFallBack() {
        val track = mangas[1].toTrack(TrackerManager.SHIKIMORI)
        track.coverUrl shouldBe ""
        track.summary shouldBe ""
        track.score shouldBe -1.0
        track.publishingStatus shouldBe ""
        track.publishingType shouldBe "oneshot"
        track.startDate shouldBe ""
        track.authors shouldBe emptyList()
    }

    @Test
    fun missingEverythingFallsBack() {
        val track = mangas[2].toTrack(TrackerManager.SHIKIMORI)
        track.score shouldBe -1.0
        track.publishingType shouldBe ""
        track.startDate shouldBe ""
        track.artists shouldBe emptyList()
    }

    @Test
    fun creditsMatchRoleSubstrings() {
        val roles = listOf(
            SMPersonRole(SMPerson("A"), listOf("Story", "Story & Art")),
            SMPersonRole(SMPerson("B"), listOf("Art")),
        )
        roles.creditedFor("Story") shouldBe listOf("A", "A")
        roles.creditedFor("Art") shouldBe listOf("A", "B")
        null.creditedFor("Art") shouldBe emptyList()
    }

    @Test
    fun searchResultRoundTrips() {
        exerciseDto(result)
        val json = TrackerHarness.json
        json.decodeFromString<SMSearchResult>(json.encodeToString(result)) shouldBe result
        mangas[0].personRoles?.get(2) shouldBe SMPersonRole(SMPerson("Only Artist"), listOf("Art", "Design"))
    }
}
