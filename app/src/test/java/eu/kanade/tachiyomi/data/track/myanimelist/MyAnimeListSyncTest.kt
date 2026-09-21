package eu.kanade.tachiyomi.data.track.myanimelist

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
internal class MyAnimeListSyncTest {

    private val server = FakeServer()
    private lateinit var mal: MyAnimeList

    @Before
    fun setUp() {
        TrackerHarness.start(server.client)
        mal = MyAnimeList(TrackerManager.MYANIMELIST)
        mal.saveCredentials("kuhy", "tok")
    }

    @After
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(
        status: Long = MyAnimeList.PLAN_TO_READ,
        lastChapterRead: Double = 0.0,
        totalChapters: Long = 0L,
    ): Track = dbTrack(
        trackerId = TrackerManager.MYANIMELIST,
        remoteId = 2L,
        status = status,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
    )

    private fun enqueueStatus() = server.enqueue(200, fixture("myanimelist", "update_status.json"))

    @Test
    fun updateWithoutReadingKeeps() = runSuspend {
        enqueueStatus()
        mal.update(track(status = MyAnimeList.ON_HOLD))
        server.bodies.single() shouldContain "status=on_hold"
        enqueueStatus()
        mal.update(track(status = MyAnimeList.COMPLETED), didReadChapter = true)
        server.bodies.last() shouldContain "status=completed"
    }

    @Test
    fun readingLastChapterCompletes() = runSuspend {
        enqueueStatus()
        val track = track(lastChapterRead = 10.0, totalChapters = 10L)
        mal.update(track, didReadChapter = true)
        server.bodies.single() shouldContain "status=completed"
        track.finishedReadingDate shouldNotBe 0L
        enqueueStatus()
        mal.update(track(lastChapterRead = 0.0, totalChapters = 0L), didReadChapter = true)
        server.bodies.last() shouldContain "status=reading"
    }

    @Test
    fun readingKeepsRereadingElseReads() = runSuspend {
        enqueueStatus()
        mal.update(track(status = MyAnimeList.REREADING, lastChapterRead = 1.0), didReadChapter = true)
        server.bodies.single() shouldContain "is_rereading=true"
        enqueueStatus()
        val first = track(lastChapterRead = 1.0, totalChapters = 10L)
        mal.update(first, didReadChapter = true)
        first.startedReadingDate shouldNotBe 0L
        enqueueStatus()
        val later = track(lastChapterRead = 5.0, totalChapters = 10L)
        mal.update(later, didReadChapter = true)
        later.startedReadingDate shouldBe 0L
    }

    @Test
    fun deleteGoesThroughApi() = runSuspend {
        server.enqueue(200)
        mal.delete(domainTrack(TrackerManager.MYANIMELIST, remoteId = 2L))
        server.lastRequest.method shouldBe "DELETE"
    }

    @Test
    fun bindToExistingEntryUpdatesIt() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "list_item.json"))
        enqueueStatus()
        val track = track(status = MyAnimeList.PLAN_TO_READ)
        mal.bind(track, hasReadChapters = true)
        track.remoteId shouldBe 2L
        track.totalChapters shouldBe 380L
        server.bodies.last() shouldContain "status=reading&is_rereading=false&score=8.0&num_chapters_read=42"
        server.enqueue(200, fixture("myanimelist", "list_item.json"))
        enqueueStatus()
        mal.bind(track())
        server.bodies.last() shouldContain "status=reading"
    }

    @Test
    fun bindKeepsCompletedAndRereading() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "list_item_rereading.json"))
        enqueueStatus()
        mal.bind(track(), hasReadChapters = true)
        server.bodies.last() shouldContain "is_rereading=true"
        server.enqueue(200, fixture("myanimelist", "list_item.json").replace("\"reading\"", "\"completed\""))
        enqueueStatus()
        mal.bind(track(), hasReadChapters = true)
        server.bodies.last() shouldContain "status=completed"
    }

    @Test
    fun bindWithoutEntryAddsOne() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "list_item_absent.json"))
        enqueueStatus()
        mal.bind(track(status = MyAnimeList.ON_HOLD), hasReadChapters = true)
        server.bodies.last() shouldContain "status=reading&is_rereading=false&score=0.0"
        server.enqueue(200, fixture("myanimelist", "list_item_absent.json"))
        enqueueStatus()
        mal.bind(track(), hasReadChapters = false)
        server.bodies.last() shouldContain "status=plan_to_read"
    }

    @Test
    fun searchRoutesByPrefix() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "manga_details.json"))
        mal.search("id:2").single().remoteId shouldBe 2L
        server.enqueue(200, fixture("myanimelist", "mangalist_page2.json"))
        mal.search("my:berserk").single().remoteId shouldBe 6L
        server.enqueue(200, fixture("myanimelist", "search.json"))
        mal.search("id:x").size shouldBe 2
        server.enqueue(200, fixture("myanimelist", "manga_details.json"))
        checkNotNull(mal.searchById("2")).title shouldBe "Berserk"
    }

    @Test
    fun refreshFallsBackToAdd() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "list_item.json"))
        mal.refresh(track()).lastChapterRead shouldBe 42.0
        server.enqueue(200, fixture("myanimelist", "list_item_absent.json"))
        enqueueStatus()
        mal.refresh(track()).status shouldBe MyAnimeList.PLAN_TO_READ
        server.enqueue(200, fixture("myanimelist", "metadata.json"))
        checkNotNull(mal.getMangaMetadata(domainTrack(TrackerManager.MYANIMELIST, remoteId = 2L))).title shouldBe
            "Berserk"
    }

    @Test
    fun loginStoresUserAndToken() = runSuspend {
        server.enqueue(200, fixture("myanimelist", "oauth.json"))
        server.enqueue(200, fixture("myanimelist", "user.json"))
        mal.login("ignored", "code")
        mal.getUsername() shouldBe "kuhy"
        mal.getPassword() shouldBe "acc"
        mal.getDisplayUsername() shouldBe "kuhy"
        checkNotNull(mal.loadOAuth()).refreshToken shouldBe "ref"
        server.enqueue(500)
        mal.login("code")
        mal.isLoggedIn shouldBe false
    }
}
