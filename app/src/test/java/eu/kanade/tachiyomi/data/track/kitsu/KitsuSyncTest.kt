package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class KitsuSyncTest {

    private val server = FakeServer()
    private lateinit var kitsu: Kitsu

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        kitsu = Kitsu(TrackerManager.KITSU)
        kitsu.saveCredentials("user", "9001")
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(
        status: Long = Kitsu.PLAN_TO_READ,
        lastChapterRead: Double = 0.0,
        totalChapters: Long = 0L,
    ): Track = dbTrack(
        trackerId = TrackerManager.KITSU,
        remoteId = 42L,
        libraryId = 501L,
        status = status,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
    )

    @Test
    fun updateWithoutReadingKeeps() = runSuspend {
        server.enqueue(200)
        val track = track(status = Kitsu.ON_HOLD)
        kitsu.update(track).status shouldBe Kitsu.ON_HOLD
        server.enqueue(200)
        kitsu.update(track(status = Kitsu.COMPLETED), didReadChapter = true).status shouldBe Kitsu.COMPLETED
    }

    @Test
    fun readingLastChapterCompletes() = runSuspend {
        server.enqueue(200)
        val track = track(status = Kitsu.PLAN_TO_READ, lastChapterRead = 10.0, totalChapters = 10L)
        kitsu.update(track, didReadChapter = true)
        track.status shouldBe Kitsu.COMPLETED
        track.finishedReadingDate shouldNotBe 0L
    }

    @Test
    fun readingFirstChapterStarts() = runSuspend {
        server.enqueue(200)
        val first = track(lastChapterRead = 1.0, totalChapters = 10L)
        kitsu.update(first, didReadChapter = true)
        first.status shouldBe Kitsu.READING
        first.startedReadingDate shouldNotBe 0L
        server.enqueue(200)
        val later = track(lastChapterRead = 5.0, totalChapters = 0L)
        kitsu.update(later, didReadChapter = true)
        later.status shouldBe Kitsu.READING
        later.startedReadingDate shouldBe 0L
        server.enqueue(200)
        kitsu.update(track(lastChapterRead = 0.0, totalChapters = 0L), didReadChapter = true).status shouldBe
            Kitsu.READING
    }

    @Test
    fun deleteRemovesRemoteEntry() = runSuspend {
        server.enqueue(204)
        kitsu.delete(domainTrack(TrackerManager.KITSU, libraryId = 501L))
        server.lastRequest.method shouldBe "DELETE"
    }

    @Test
    fun bindToExistingEntryUpdatesIt() = runSuspend {
        server.enqueue(200, fixture("kitsu", "library_entries.json"))
        server.enqueue(200)
        val track = track(status = Kitsu.PLAN_TO_READ)
        kitsu.bind(track, hasReadChapters = true)
        track.remoteId shouldBe 42L
        track.libraryId shouldBe 501L
        track.status shouldBe Kitsu.READING
        track.score shouldBe 7.5
        track.private shouldBe false
        server.requests.size shouldBe 2
        server.lastRequest.method shouldBe "PATCH"
    }

    @Test
    fun bindKeepsRemoteStatus() = runSuspend {
        server.enqueue(200, fixture("kitsu", "library_entries.json"))
        server.enqueue(200)
        kitsu.bind(track()).status shouldBe Kitsu.READING
        server.enqueue(200, fixture("kitsu", "library_entries_completed.json"))
        server.enqueue(200)
        val completed = track(status = Kitsu.PLAN_TO_READ)
        kitsu.bind(completed, hasReadChapters = true)
        completed.status shouldBe Kitsu.COMPLETED
    }

    @Test
    fun bindWithoutEntryAddsOne() = runSuspend {
        server.enqueue(200, fixture("kitsu", "library_entries_empty.json"))
        server.enqueue(200, fixture("kitsu", "add_manga.json"))
        val track = track(status = Kitsu.ON_HOLD)
        kitsu.bind(track, hasReadChapters = true)
        track.status shouldBe Kitsu.READING
        track.libraryId shouldBe 77L
        server.bodies[1] shouldContain """"id":"9001""""
        server.enqueue(200, fixture("kitsu", "library_entries_empty.json"))
        server.enqueue(200, fixture("kitsu", "add_manga.json"))
        kitsu.bind(track(), hasReadChapters = false).status shouldBe Kitsu.PLAN_TO_READ
    }

    @Test
    fun searchAndRefreshGoThroughApi() = runSuspend {
        server.enqueue(200, fixture("kitsu", "algolia_key.json"))
        server.enqueue(200, fixture("kitsu", "algolia_search.json"))
        kitsu.search("one").size shouldBe 2
        server.enqueue(200, fixture("kitsu", "library_entries.json"))
        val track = track()
        kitsu.refresh(track)
        track.totalChapters shouldBe 1000L
        track.lastChapterRead shouldBe 12.0
        track.private shouldBe true
    }

    @Test
    fun loginStoresUserAndToken() = runSuspend {
        server.enqueue(200, fixture("kitsu", "oauth.json"))
        server.enqueue(200, fixture("kitsu", "users.json"))
        kitsu.login("mail", "pass")
        kitsu.getUsername() shouldBe "mail"
        kitsu.getPassword() shouldBe "9001"
        kitsu.getDisplayUsername() shouldBe "kuhy"
        checkNotNull(kitsu.restoreToken()).accessToken shouldBe "acc"
    }

    @Test
    fun metadataGoesThroughApi() = runSuspend {
        server.enqueue(200, fixture("kitsu", "metadata.json"))
        kitsu.getMangaMetadata(domainTrack(TrackerManager.KITSU, remoteId = 42L)).title shouldBe "One Piece"
    }
}
