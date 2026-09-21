package eu.kanade.tachiyomi.data.track.bangumi

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

internal class BangumiSyncTest {

    private val server = FakeServer()
    private lateinit var bangumi: Bangumi

    @BeforeEach
    fun setUp() {
        TrackerHarness.start(server.client)
        bangumi = Bangumi(TrackerManager.BANGUMI)
        bangumi.saveCredentials("kuhy42", "tok")
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    private fun track(
        status: Long = Bangumi.PLAN_TO_READ,
        lastChapterRead: Double = 0.0,
        totalChapters: Long = 0L,
    ): Track = dbTrack(
        trackerId = TrackerManager.BANGUMI,
        remoteId = 1L,
        status = status,
        lastChapterRead = lastChapterRead,
        totalChapters = totalChapters,
    )

    @Test
    fun updateWithoutReadingKeeps() = runSuspend {
        server.enqueue(204)
        bangumi.update(track(status = Bangumi.ON_HOLD)).status shouldBe Bangumi.ON_HOLD
        server.lastRequest.method shouldBe "PATCH"
        server.enqueue(204)
        bangumi.update(track(status = Bangumi.COMPLETED), didReadChapter = true).status shouldBe Bangumi.COMPLETED
    }

    @Test
    fun readingLastChapterCompletes() = runSuspend {
        server.enqueue(204)
        bangumi.update(track(lastChapterRead = 10.0, totalChapters = 10L), didReadChapter = true).status shouldBe
            Bangumi.COMPLETED
        server.enqueue(204)
        bangumi.update(track(lastChapterRead = 0.0, totalChapters = 0L), didReadChapter = true).status shouldBe
            Bangumi.READING
        server.enqueue(204)
        bangumi.update(track(lastChapterRead = 5.0, totalChapters = 10L), didReadChapter = true).status shouldBe
            Bangumi.READING
    }

    @Test
    fun bindToExistingEntryUpdatesIt() = runSuspend {
        server.enqueue(200, fixture("bangumi", "collection.json"))
        server.enqueue(204)
        val track = track(status = Bangumi.PLAN_TO_READ).apply { private = false }
        bangumi.bind(track, hasReadChapters = true)
        track.status shouldBe Bangumi.READING
        track.score shouldBe 8.0
        track.lastChapterRead shouldBe 12.0
        track.totalChapters shouldBe 276L
        track.private shouldBe false
        server.bodies.last() shouldContain """"type":3"""
        server.enqueue(200, fixture("bangumi", "collection.json").replace("\"type\": 3", "\"type\": 4"))
        server.enqueue(204)
        bangumi.bind(track()).status shouldBe Bangumi.ON_HOLD
    }

    @Test
    fun bindKeepsCompleted() = runSuspend {
        server.enqueue(200, fixture("bangumi", "collection.json").replace("\"type\": 3", "\"type\": 2"))
        server.enqueue(204)
        bangumi.bind(track(), hasReadChapters = true).status shouldBe Bangumi.COMPLETED
    }

    @Test
    fun bindWithoutEntryAddsOne() = runSuspend {
        server.enqueue(404)
        server.enqueue(202)
        val track = track(status = Bangumi.ON_HOLD)
        bangumi.bind(track, hasReadChapters = true)
        track.status shouldBe Bangumi.READING
        server.lastRequest.method shouldBe "POST"
        server.enqueue(404)
        server.enqueue(202)
        bangumi.bind(track(), hasReadChapters = false).status shouldBe Bangumi.PLAN_TO_READ
    }

    @Test
    fun refreshRequiresCollection() = runSuspend {
        server.enqueue(200, fixture("bangumi", "collection.json"))
        val track = track()
        bangumi.refresh(track)
        track.status shouldBe Bangumi.READING
        track.lastChapterRead shouldBe 12.0
        server.enqueue(404)
        shouldThrow<NoSuchElementException> { bangumi.refresh(track()) }
    }

    @Test
    fun searchAndMetadataGoThroughApi() = runSuspend {
        server.enqueue(200, fixture("bangumi", "search.json"))
        bangumi.search("slam").size shouldBe 2
        server.enqueue(200, fixture("bangumi", "subject.json"))
        checkNotNull(bangumi.getMangaMetadata(domainTrack(TrackerManager.BANGUMI, remoteId = 1L))).title shouldBe
            "灌篮高手"
    }

    @Test
    fun loginPrefersNickname() = runSuspend {
        server.enqueue(200, fixture("bangumi", "oauth.json"))
        server.enqueue(200, fixture("bangumi", "user.json"))
        bangumi.login("ignored", "code")
        bangumi.getUsername() shouldBe "kuhy42"
        bangumi.getPassword() shouldBe "acc"
        bangumi.getDisplayUsername() shouldBe "kuhy"
        checkNotNull(bangumi.restoreToken()).refreshToken shouldBe "ref"
    }

    @Test
    fun loginFallsBackToUsername() = runSuspend {
        server.enqueue(200, fixture("bangumi", "oauth.json"))
        server.enqueue(200, fixture("bangumi", "user.json").replace("\"kuhy\"", "\" \""))
        bangumi.login("code")
        bangumi.getDisplayUsername() shouldBe "kuhy42"
        server.enqueue(200, fixture("bangumi", "oauth.json"))
        server.enqueue(200, fixture("bangumi", "user.json").replace("\"kuhy\"", "null"))
        bangumi.login("code")
        bangumi.getDisplayUsername() shouldBe "kuhy42"
    }

    @Test
    fun failedLoginLogsOut() = runSuspend {
        server.enqueue(500)
        bangumi.login("code")
        bangumi.isLoggedIn shouldBe false
    }
}
