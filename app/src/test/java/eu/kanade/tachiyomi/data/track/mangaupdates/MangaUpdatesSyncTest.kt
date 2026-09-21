package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import okhttp3.Headers.Companion.headersOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MangaUpdatesSyncTest {

    private val server = FakeServer()
    private lateinit var mangaUpdates: MangaUpdates

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        mangaUpdates = MangaUpdates(TrackerManager.MANGAUPDATES)
        mangaUpdates.saveCredentials("555", "sess")
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(status: Long = MangaUpdates.WISH_LIST, score: Double = 8.5): Track = dbTrack(
        trackerId = TrackerManager.MANGAUPDATES,
        remoteId = 87_654_321L,
        status = status,
        lastChapterRead = 42.0,
        score = score,
    )

    @Test
    fun updateMovesToReadingList() = runSuspend {
        server.enqueue(200)
        server.enqueue(200)
        mangaUpdates.update(track(), didReadChapter = true).status shouldBe MangaUpdates.READING_LIST
        server.enqueue(200)
        server.enqueue(200)
        mangaUpdates.update(track()).status shouldBe MangaUpdates.WISH_LIST
        server.enqueue(200)
        server.enqueue(200)
        val complete = track(status = MangaUpdates.COMPLETE_LIST)
        mangaUpdates.update(complete, didReadChapter = true).status shouldBe MangaUpdates.COMPLETE_LIST
    }

    @Test
    fun deleteGoesThroughApi() = runSuspend {
        server.enqueue(200)
        mangaUpdates.delete(domainTrack(TrackerManager.MANGAUPDATES, remoteId = 87_654_321L))
        server.lastRequest.url.toString() shouldBe "https://api.mangaupdates.com/v1/lists/series/delete"
    }

    @Test
    fun bindCopiesExistingEntry() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "list_item.json"))
        server.enqueue(200, fixture("mangaupdates", "rating.json"))
        val track = mangaUpdates.bind(track(score = 0.0), hasReadChapters = true)
        track.status shouldBe MangaUpdates.COMPLETE_LIST
        track.lastChapterRead shouldBe 42.0
        track.score shouldBe 8.5
        server.enqueue(200, fixture("mangaupdates", "list_item.json"))
        server.enqueue(404)
        mangaUpdates.bind(track()).score shouldBe 0.0
    }

    @Test
    fun bindAddsMissingEntry() = runSuspend {
        server.enqueue(404)
        server.enqueue(200)
        val track = mangaUpdates.bind(track(), hasReadChapters = true)
        track.status shouldBe MangaUpdates.READING_LIST
        track.score shouldBe 0.0
        track.lastChapterRead shouldBe 1.0
        server.enqueue(404)
        server.enqueue(200)
        mangaUpdates.bind(track(), hasReadChapters = false).status shouldBe MangaUpdates.WISH_LIST
    }

    @Test
    fun searchAndRefresh() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "search.json"))
        val results = mangaUpdates.search("berserk")
        results.size shouldBe 3
        results[0].trackerId shouldBe TrackerManager.MANGAUPDATES
        server.enqueue(200, fixture("mangaupdates", "list_item.json"))
        server.enqueue(200, fixture("mangaupdates", "rating_null.json"))
        val refreshed = mangaUpdates.refresh(track())
        refreshed.status shouldBe MangaUpdates.COMPLETE_LIST
        refreshed.score shouldBe 0.0
    }

    @Test
    fun loginStoresSession() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "login.json"))
        server.enqueue(200, fixture("mangaupdates", "profile.json"))
        mangaUpdates.login("kuhy", "pass")
        mangaUpdates.getUsername() shouldBe "555"
        mangaUpdates.getPassword() shouldBe "sess"
        mangaUpdates.getDisplayUsername() shouldBe "kuhy"
        server.requests[1].header("Authorization") shouldBe null
    }

    @Test
    fun metadataSplitsCredits() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "series.json"))
        mangaUpdates.getMangaMetadata(domainTrack(TrackerManager.MANGAUPDATES, remoteId = 87_654_321L)) shouldBe
            TrackMangaMetadata(
                remoteId = 87_654_321L,
                title = "Berserk & Co",
                thumbnailUrl = "https://img/o.jpg",
                description = "Guts & Griffith",
                authors = "Kentarou Miura, ",
                artists = "Kentarou Miura, ",
            )
    }

    @Test
    fun metadataWithoutAuthors() = runSuspend {
        server.enqueue(200, fixture("mangaupdates", "series_bare.json"))
        val bare = checkNotNull(mangaUpdates.getMangaMetadata(domainTrack(TrackerManager.MANGAUPDATES, remoteId = 2L)))
        bare.thumbnailUrl.shouldBeNull()
        bare.description.shouldBeNull()
        bare.authors.shouldBeNull()
        bare.artists.shouldBeNull()
        server.enqueue(200, fixture("mangaupdates", "series_empty.json"))
        val empty = checkNotNull(mangaUpdates.getMangaMetadata(domainTrack(TrackerManager.MANGAUPDATES)))
        empty.remoteId.shouldBeNull()
        empty.title.shouldBeNull()
    }

    @Test
    fun searchByIdConvertsLegacyIds() = runSuspend {
        server.enqueue(308, headers = headersOf("Location", "https://www.mangaupdates.com/series/abc123/berserk"))
        server.enqueue(200, fixture("mangaupdates", "series.json"))
        checkNotNull(mangaUpdates.searchById("15")).title shouldBe "Berserk & Co"
        server.requests[1].url.toString() shouldBe "https://api.mangaupdates.com/v1/series/${"abc123".toLong(36)}"
        server.enqueue(200)
        mangaUpdates.searchById("15").shouldBeNull()
        server.enqueue(200, fixture("mangaupdates", "series_bare.json"))
        checkNotNull(mangaUpdates.searchById("q9")).title shouldBe "Bare"
        server.lastRequest.url.toString() shouldBe "https://api.mangaupdates.com/v1/series/${"q9".toLong(36)}"
    }
}
