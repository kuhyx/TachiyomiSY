package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AnilistSyncTest {

    private val server = FakeServer()
    private lateinit var anilist: Anilist

    @BeforeEach
    fun setUp() {
        TrackerHarness.start(server.client)
        anilist = Anilist(TrackerManager.ANILIST)
        anilist.saveCredentials("777", "tok")
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(
        status: Long = Anilist.PLAN_TO_READ,
        libraryId: Long? = 5001L,
        lastChapterRead: Double = 0.0,
        totalChapters: Long = 0L,
    ): Track = dbTrack(
        trackerId = TrackerManager.ANILIST,
        remoteId = 101L,
        libraryId = libraryId,
        status = status,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
    )

    @Test
    fun updateLooksUpMissingLibraryId() = runSuspend {
        server.enqueue(200, fixture("anilist", "user_list.json"))
        server.enqueue(200)
        anilist.update(track(libraryId = null)).libraryId shouldBe 5001L
        server.enqueue(200, fixture("anilist", "user_list.json"))
        server.enqueue(200)
        anilist.update(track(libraryId = 0L)).libraryId shouldBe 5001L
        server.enqueue(200, fixture("anilist", "user_list_empty.json"))
        shouldThrow<NoSuchElementException> { anilist.update(track(libraryId = null)) }
    }

    @Test
    fun updateWithoutReadingKeeps() = runSuspend {
        server.enqueue(200)
        anilist.update(track(status = Anilist.ON_HOLD)).status shouldBe Anilist.ON_HOLD
        server.enqueue(200)
        anilist.update(track(status = Anilist.COMPLETED), didReadChapter = true).status shouldBe Anilist.COMPLETED
    }

    @Test
    fun readingLastChapterCompletes() = runSuspend {
        server.enqueue(200)
        val track = track(lastChapterRead = 10.0, totalChapters = 10L)
        anilist.update(track, didReadChapter = true)
        track.status shouldBe Anilist.COMPLETED
        track.finishedReadingDate shouldNotBe 0L
        server.enqueue(200)
        anilist.update(track(lastChapterRead = 0.0, totalChapters = 0L), didReadChapter = true).status shouldBe
            Anilist.READING
    }

    @Test
    fun readingKeepsRereadingElseReads() = runSuspend {
        server.enqueue(200)
        anilist.update(track(status = Anilist.REREADING, lastChapterRead = 1.0), didReadChapter = true).status shouldBe
            Anilist.REREADING
        server.enqueue(200)
        val first = track(lastChapterRead = 1.0, totalChapters = 10L)
        anilist.update(first, didReadChapter = true)
        first.status shouldBe Anilist.READING
        first.startedReadingDate shouldNotBe 0L
        server.enqueue(200)
        val later = track(lastChapterRead = 5.0, totalChapters = 10L)
        anilist.update(later, didReadChapter = true)
        later.startedReadingDate shouldBe 0L
    }

    @Test
    fun deleteResolvesLibraryIdFirst() = runSuspend {
        server.enqueue(200)
        anilist.delete(domainTrack(TrackerManager.ANILIST, remoteId = 101L, libraryId = 5001L))
        server.bodies.single() shouldContain """"listId":5001"""
        server.enqueue(200, fixture("anilist", "user_list.json"))
        server.enqueue(200)
        anilist.delete(domainTrack(TrackerManager.ANILIST, remoteId = 101L, libraryId = null))
        server.requests.size shouldBe 3
        // The looked-up entry id lands in `id`, not `libraryId`, so the mutation still carries a null list id.
        server.bodies.last() shouldContain "mutation DeleteManga"
        server.enqueue(200, fixture("anilist", "user_list_empty.json"))
        anilist.delete(domainTrack(TrackerManager.ANILIST, remoteId = 101L, libraryId = 0L))
        server.requests.size shouldBe 4
        server.enqueue(200, fixture("anilist", "user_list.json"))
        server.enqueue(200)
        anilist.delete(domainTrack(TrackerManager.ANILIST, remoteId = 101L, libraryId = 0L))
        server.requests.size shouldBe 6
    }

    @Test
    fun bindToExistingEntryUpdatesIt() = runSuspend {
        server.enqueue(200, fixture("anilist", "user_list.json"))
        server.enqueue(200)
        val track = track(status = Anilist.PLAN_TO_READ, libraryId = null)
        anilist.bind(track, hasReadChapters = true)
        track.libraryId shouldBe 5001L
        track.status shouldBe Anilist.READING
        track.score shouldBe 80.0
        track.private shouldBe false
        server.enqueue(200, fixture("anilist", "user_list.json"))
        server.enqueue(200)
        anilist.bind(track()).status shouldBe Anilist.READING
    }

    @Test
    fun bindKeepsCompletedAndRereading() = runSuspend {
        server.enqueue(200, fixture("anilist", "user_list.json").replace("CURRENT", "COMPLETED"))
        server.enqueue(200)
        anilist.bind(track(), hasReadChapters = true).status shouldBe Anilist.COMPLETED
        server.enqueue(200, fixture("anilist", "user_list.json").replace("CURRENT", "REPEATING"))
        server.enqueue(200)
        anilist.bind(track(), hasReadChapters = true).status shouldBe Anilist.REREADING
    }

    @Test
    fun bindWithoutEntryAddsOne() = runSuspend {
        server.enqueue(200, fixture("anilist", "user_list_empty.json"))
        server.enqueue(200, fixture("anilist", "add_manga.json"))
        val track = track(status = Anilist.ON_HOLD, libraryId = null)
        anilist.bind(track, hasReadChapters = true)
        track.status shouldBe Anilist.READING
        track.libraryId shouldBe 5001L
        server.enqueue(200, fixture("anilist", "user_list_empty.json"))
        server.enqueue(200, fixture("anilist", "add_manga.json"))
        anilist.bind(track(), hasReadChapters = false).status shouldBe Anilist.PLAN_TO_READ
    }

    @Test
    fun refreshCopiesRemoteState() = runSuspend {
        server.enqueue(200, fixture("anilist", "user_list.json"))
        val track = track()
        anilist.refresh(track)
        track.title shouldBe "Solo Leveling"
        track.totalChapters shouldBe 179L
        track.lastChapterRead shouldBe 12.0
        track.private shouldBe true
        server.enqueue(200, fixture("anilist", "user_list_empty.json"))
        shouldThrow<NoSuchElementException> { anilist.refresh(track()) }
    }

    @Test
    fun loginStoresUserAndFormat() = runSuspend {
        server.enqueue(200, fixture("anilist", "current_user.json"))
        anilist.login("ignored", "token")
        anilist.getUsername() shouldBe "777"
        anilist.getPassword() shouldBe "token"
        anilist.getDisplayUsername() shouldBe "kuhy"
        anilist.scorePreference.get() shouldBe Anilist.POINT_100
        checkNotNull(anilist.loadOAuth()).accessToken shouldBe "token"
        server.enqueue(500)
        anilist.login("token2")
        anilist.isLoggedIn shouldBe false
    }
}
