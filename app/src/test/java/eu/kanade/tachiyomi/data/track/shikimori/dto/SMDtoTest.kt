package eu.kanade.tachiyomi.data.track.shikimori.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.exerciseDto
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SMDtoTest {

    private val json = TrackerHarness.json

    private inline fun <reified T> roundTrip(name: String): T {
        val decoded = json.decodeFromString<T>(fixture("shikimori", name))
        exerciseDto(decoded)
        json.decodeFromString<T>(json.encodeToString(decoded)) shouldBe decoded
        return decoded
    }

    @Test
    fun userListEntryMapsToTrack() {
        val manga = roundTrip<SMUserListResult>("user_list.json").data.mangas.single()
        val track = manga.toTrack(TrackerManager.SHIKIMORI)
        track.trackerId shouldBe TrackerManager.SHIKIMORI
        track.title shouldBe "Berserk"
        track.totalChapters shouldBe 380L
        track.trackingUrl shouldBe "https://shikimori.io/mangas/2-berserk"
        track.remoteId shouldBe 9001L
        track.libraryId shouldBe 9001L
        track.lastChapterRead shouldBe 42.0
        track.score shouldBe 8.0
        track.status shouldBe Shikimori.READING
    }

    @Test
    fun userListEntryWithoutRate() {
        val manga = roundTrip<SMUserListResult>("user_list_no_rate.json").data.mangas.single()
        val track = manga.toTrack(TrackerManager.SHIKIMORI)
        track.remoteId shouldBe 0L
        track.libraryId shouldBe null
        track.status shouldBe 0L
        roundTrip<SMUserListResult>("user_list_empty.json").data.mangas shouldBe emptyList()
    }

    @Test
    fun oauthExpiry() {
        val decoded = roundTrip<SMOAuth>("oauth.json")
        decoded shouldBe SMOAuth("acc", "Bearer", 1_700_000_000L, 86_400L, "ref")
        decoded.isExpired() shouldBe true
        decoded.copy(createdAt = System.currentTimeMillis() / 1000L).isExpired() shouldBe false
        decoded.copy(createdAt = System.currentTimeMillis() / 1000L, expiresIn = 3599L).isExpired() shouldBe true
    }

    @Test
    fun userAndAddResponses() {
        roundTrip<SMUserResult>("user.json") shouldBe SMUserResult(SMCurrentUser(SMUser("31337", "kuhy")))
        roundTrip<SMAddMangaResponse>("add_manga.json") shouldBe SMAddMangaResponse(9001L)
    }

    @Test
    fun metadataDecodes() {
        val decoded = roundTrip<SMMetadata>("metadata.json")
        val manga = decoded.data.mangas.single()
        manga.id shouldBe "2"
        manga.poster shouldBe SMMangaPoster("https://img/2o.jpg")
        manga.personRoles.size shouldBe 4
    }
}
