package eu.kanade.tachiyomi.data.track.mdlist

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.source.model.MangasPage
import exh.md.utils.FollowStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

/** The [MdList] tracker's vocabulary, login state, search and metadata. */
internal class MdListTest {

    private val harness = MdListHarness()
    private val tracker: MdList
        get() = harness.tracker

    @BeforeEach
    fun setUp() {
        harness.start()
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun identityAndVocabulary() {
        tracker.id shouldBe 60L
        tracker.name shouldBe "MDList"
        tracker.getLogo() shouldBe R.drawable.brand_mangadex
        tracker.getStatusList() shouldBe (0L..6L).toList()
        tracker.getStatus(FollowStatus.UNFOLLOWED.long) shouldBe SYMR.strings.md_follows_unfollowed
        tracker.getStatus(FollowStatus.READING.long) shouldBe MR.strings.reading
        tracker.getStatus(FollowStatus.COMPLETED.long) shouldBe MR.strings.completed
        tracker.getStatus(FollowStatus.ON_HOLD.long) shouldBe MR.strings.on_hold
        tracker.getStatus(FollowStatus.PLAN_TO_READ.long) shouldBe MR.strings.plan_to_read
        tracker.getStatus(FollowStatus.DROPPED.long) shouldBe MR.strings.dropped
        tracker.getStatus(FollowStatus.RE_READING.long) shouldBe MR.strings.repeating
        tracker.getStatus(42L).shouldBeNull()
        tracker.getScoreList() shouldBe (0..10).map { it.toString() }
        tracker.displayScore(domainTrack(score = 7.8)) shouldBe "7"
        tracker.getCompletionStatus() shouldBe FollowStatus.COMPLETED.long
        tracker.getReadingStatus() shouldBe FollowStatus.READING.long
        tracker.getRereadingStatus() shouldBe FollowStatus.RE_READING.long
    }

    @Test
    fun loginStateIsTheToken() = runTest {
        tracker.isLoggedIn shouldBe false
        harness.koin.trackPreferences.trackToken(tracker).set("token")
        tracker.isLoggedIn shouldBe true
        tracker.interceptor.token shouldBe null
        shouldThrow<UnsupportedOperationException> { tracker.login("u", "p") }.message shouldBe
            "MDList signs in through the MangaDex source"
        tracker.logout()
        tracker.isLoggedIn shouldBe false
        harness.koin.trackPreferences.trackToken(tracker).isSet() shouldBe false
    }

    @Test
    fun initialTrackerPointsAtMangaDex() {
        val dbManga = Manga.create().copy(id = 7L, url = "/manga/uuid-1", ogTitle = "Local")
        val fromDb = tracker.createInitialTracker(dbManga)
        fromDb.trackerId shouldBe 60L
        fromDb.mangaId shouldBe 7L
        fromDb.status shouldBe FollowStatus.UNFOLLOWED.long
        fromDb.trackingUrl shouldBe "https://mangadex.org/manga/uuid-1"
        fromDb.title shouldBe "Local"

        val mdManga = Manga.create().copy(id = 8L, url = "/manga/uuid-2", ogTitle = "Remote")
        val fromMd = tracker.createInitialTracker(dbManga, mdManga)
        fromMd.mangaId shouldBe 7L
        fromMd.trackingUrl shouldBe "https://mangadex.org/manga/uuid-2"
        fromMd.title shouldBe "Remote"
    }

    @Test
    fun searchResolvesAndDedupes() = runTest {
        val listed = MdListHarness.sManga(url = "/manga/a", title = "A")
        val page = MangasPage(listOf(listed, listed), false)
        coEvery { harness.mangaDex.getSearchManga(1, "query", any()) } returns page
        coEvery { harness.mangaDex.getMangaDetails(listed) } returns
            MdListHarness.sManga(url = "/manga/a", title = "A full", thumbnail = "cover", description = "desc")

        val results = tracker.search("query")
        results.size shouldBe 1
        val result = results.single()
        result.trackerId shouldBe 60L
        result.trackingUrl shouldBe "https://mangadex.org/manga/a"
        result.title shouldBe "A full"
        result.coverUrl shouldBe "cover"
        result.summary shouldBe "desc"
    }

    @Test
    fun searchDefaultsMissingFields() = runTest {
        val listed = MdListHarness.sManga(url = "/manga/b", title = "B")
        coEvery { harness.mangaDex.getSearchManga(1, "b", any()) } returns
            MangasPage(listOf(listed), false)
        coEvery { harness.mangaDex.getMangaDetails(listed) } returns listed
        val result = tracker.search("b").single()
        result.coverUrl shouldBe ""
        result.summary shouldBe ""
    }

    @Test
    fun metadataComesFromMangaDex() = runTest {
        val manga = MdListHarness.sManga(url = "/manga/c", title = "C", thumbnail = "thumb", description = "d").also {
            it.author = "writer"
            it.artist = "drawer"
        }
        coEvery { harness.mangaDex.getMangaMetadata(any()) } returns manga
        val metadata = tracker.getMangaMetadata(domainTrack(trackerId = 60L))
        metadata.remoteId shouldBe 0L
        metadata.title shouldBe "C"
        metadata.thumbnailUrl shouldBe "thumb"
        metadata.description shouldBe "d"
        metadata.authors shouldBe "writer"
        metadata.artists shouldBe "drawer"
    }
}
