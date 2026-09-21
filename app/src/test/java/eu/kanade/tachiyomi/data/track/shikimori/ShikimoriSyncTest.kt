package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.FakeServer
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.dbTrack
import eu.kanade.tachiyomi.data.track.anilist.domainTrack
import eu.kanade.tachiyomi.data.track.anilist.fixture
import eu.kanade.tachiyomi.data.track.anilist.runSuspend
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ShikimoriSyncTest {

    private val server = FakeServer()
    private lateinit var shikimori: Shikimori

    @BeforeEach
    fun setUp() {
        TrackerHarness.start(server.client)
        shikimori = Shikimori(TrackerManager.SHIKIMORI)
        shikimori.saveCredentials("31337", "tok")
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(
        status: Long = Shikimori.PLAN_TO_READ,
        lastChapterRead: Double = 0.0,
        totalChapters: Long = 0L,
    ): Track = dbTrack(
        trackerId = TrackerManager.SHIKIMORI,
        remoteId = 2L,
        status = status,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
    )

    private fun enqueueAdd() = server.enqueue(200, fixture("shikimori", "add_manga.json"))

    @Test
    fun updateWithoutReadingKeeps() = runSuspend {
        enqueueAdd()
        shikimori.update(track(status = Shikimori.ON_HOLD)).status shouldBe Shikimori.ON_HOLD
        server.bodies.single() shouldContain """"user_id":"31337""""
        enqueueAdd()
        shikimori.update(track(status = Shikimori.COMPLETED), didReadChapter = true).status shouldBe
            Shikimori.COMPLETED
    }

    @Test
    fun readingLastChapterCompletes() = runSuspend {
        enqueueAdd()
        shikimori.update(track(lastChapterRead = 10.0, totalChapters = 10L), didReadChapter = true).status shouldBe
            Shikimori.COMPLETED
        enqueueAdd()
        shikimori.update(track(lastChapterRead = 0.0, totalChapters = 0L), didReadChapter = true).status shouldBe
            Shikimori.READING
        enqueueAdd()
        shikimori.update(track(lastChapterRead = 5.0, totalChapters = 10L), didReadChapter = true).status shouldBe
            Shikimori.READING
    }

    @Test
    fun readingKeepsRereading() = runSuspend {
        enqueueAdd()
        val rereading = track(status = Shikimori.REREADING, lastChapterRead = 1.0)
        shikimori.update(rereading, didReadChapter = true).status shouldBe Shikimori.REREADING
    }

    @Test
    fun deleteGoesThroughApi() = runSuspend {
        server.enqueue(200)
        shikimori.delete(domainTrack(TrackerManager.SHIKIMORI, libraryId = 9001L))
        server.lastRequest.method shouldBe "DELETE"
    }

    @Test
    fun bindToExistingEntryUpdatesIt() = runSuspend {
        server.enqueue(200, fixture("shikimori", "user_list.json"))
        enqueueAdd()
        val track = track(status = Shikimori.PLAN_TO_READ)
        shikimori.bind(track, hasReadChapters = true)
        track.libraryId shouldBe 9001L
        track.status shouldBe Shikimori.READING
        track.lastChapterRead shouldBe 42.0
        server.enqueue(200, fixture("shikimori", "user_list.json"))
        enqueueAdd()
        shikimori.bind(track()).status shouldBe Shikimori.READING
    }

    @Test
    fun bindKeepsCompletedAndRereading() = runSuspend {
        server.enqueue(200, fixture("shikimori", "user_list.json").replace("watching", "completed"))
        enqueueAdd()
        shikimori.bind(track(), hasReadChapters = true).status shouldBe Shikimori.COMPLETED
        server.enqueue(200, fixture("shikimori", "user_list.json").replace("watching", "rewatching"))
        enqueueAdd()
        shikimori.bind(track(), hasReadChapters = true).status shouldBe Shikimori.REREADING
    }

    @Test
    fun bindWithoutEntryAddsOne() = runSuspend {
        server.enqueue(200, fixture("shikimori", "user_list_empty.json"))
        enqueueAdd()
        val track = track(status = Shikimori.ON_HOLD)
        shikimori.bind(track, hasReadChapters = true)
        track.status shouldBe Shikimori.READING
        track.libraryId shouldBe 9001L
        server.enqueue(200, fixture("shikimori", "user_list_empty.json"))
        enqueueAdd()
        shikimori.bind(track(), hasReadChapters = false).status shouldBe Shikimori.PLAN_TO_READ
    }

    @Test
    fun refreshRequiresRemoteRate() = runSuspend {
        server.enqueue(200, fixture("shikimori", "user_list.json"))
        val track = track()
        shikimori.refresh(track)
        track.libraryId shouldBe 9001L
        track.totalChapters shouldBe 380L
        track.score shouldBe 8.0
        server.enqueue(200, fixture("shikimori", "user_list_no_rate.json"))
        shouldThrow<NoSuchElementException> { shikimori.refresh(track()) }
    }

    @Test
    fun searchAndMetadataGoThroughApi() = runSuspend {
        server.enqueue(200, fixture("shikimori", "search.json"))
        shikimori.search("berserk").size shouldBe 3
        server.enqueue(200, fixture("shikimori", "metadata.json"))
        checkNotNull(shikimori.getMangaMetadata(domainTrack(TrackerManager.SHIKIMORI, remoteId = 2L))).title shouldBe
            "Berserk"
    }

    @Test
    fun loginStoresUserAndToken() = runSuspend {
        server.enqueue(200, fixture("shikimori", "oauth.json"))
        server.enqueue(200, fixture("shikimori", "user.json"))
        shikimori.login("ignored", "code")
        shikimori.getUsername() shouldBe "31337"
        shikimori.getPassword() shouldBe "acc"
        shikimori.getDisplayUsername() shouldBe "kuhy"
        checkNotNull(shikimori.restoreToken()).refreshToken shouldBe "ref"
        server.enqueue(500)
        shikimori.login("code")
        shikimori.isLoggedIn shouldBe false
    }
}
