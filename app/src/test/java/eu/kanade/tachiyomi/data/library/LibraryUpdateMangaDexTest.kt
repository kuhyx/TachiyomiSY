package eu.kanade.tachiyomi.data.library

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.md.utils.FollowStatus
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.source.mangaDexSourceIds
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import kotlin.coroutines.cancellation.CancellationException
import tachiyomi.domain.track.model.Track as DomainTrack

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateMangaDexTest {

    private val harness = LibraryJobHarness()
    private val mangaDex = mockk<MangaDex>()

    @Before
    fun setUp() {
        harness.start()
        harness.securityPreferences.hideNotificationContent.set(true)
        every { mangaDex.id } returns MD
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { any<MdUtil>().getEnabledMangaDex(any<SourcePreferences>(), any<SourceManager>()) } returns mangaDex
        coEvery {
            harness.updateMangaFromRemote(
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } answers { Result.success(RemoteMangaUpdate(firstArg(), emptyList())) }
    }

    @After
    fun tearDown() = harness.stop()

    private fun follow(url: String, status: Int) = SManga.create().apply {
        this.url = url
        title = url
    } to MangaDexSearchMetadata().apply { followStatus = status }

    @Test
    fun followsJoinTheLibrary() = runTest {
        harness.sourcePreferences.mangadexSyncToLibraryIndexes.set(setOf("1"))
        val fresh = follow("/new", status = 1)
        val unfavorited = follow("/old", status = 1)
        val owned = follow("/own", status = 1)
        coEvery { mangaDex.fetchAllFollows() } returns listOf(fresh, unfavorited, owned, follow("/skip", status = 2))
        coEvery { harness.getManga.await(any<String>(), MD) } returns null
        coEvery { harness.getManga.await("/old", MD) } returns libManga(2, source = MD).copy(favorite = false)
        coEvery { harness.getManga.await("/own", MD) } returns libManga(3, source = MD)
        coEvery { harness.networkToLocalManga(any<Manga>()) } returns libManga(1, source = MD)
        harness.job().syncFollows()
        coVerify(exactly = 1) { harness.networkToLocalManga(any<Manga>()) }
        coVerify(exactly = 1) { harness.updateManga.awaitUpdateFavorite(any(), any()) }
        coVerify { harness.updateManga.awaitUpdateFavorite(2L, true) }
        fresh.second.mangaId shouldBe 1L
        coVerify(exactly = 3) { harness.insertFlatMetadata.await(any<RaisedSearchMetadata>()) }
    }

    @Test
    fun noMangaDexNoFollows() = runTest {
        every { any<MdUtil>().getEnabledMangaDex(any<SourcePreferences>(), any<SourceManager>()) } returns null
        harness.job().syncFollows()
        coVerify(exactly = 0) { mangaDex.fetchAllFollows() }
    }

    @Test
    fun pushNeedsALogin() = runTest {
        coEvery { harness.getFavorites.await() } returns listOf(libManga(1, source = MD))
        harness.job().pushFavorites()
        coVerify(exactly = 0) { harness.getTracks.await(any<Long>()) }
    }

    @Test
    fun pushFollowsUnfollowed() = runTest {
        every { harness.mdList.isLoggedIn } returns true
        val previous = mangaDexSourceIds
        mangaDexSourceIds = listOf(MD)
        try {
            coEvery { harness.getFavorites.await() } returns
                listOf(libManga(1, source = MD), libManga(2, source = MD), libManga(3, source = MD), libManga(4))
            coEvery { harness.getTracks.await(1L) } returns emptyList()
            coEvery { harness.getTracks.await(2L) } returns
                listOf(mdTrack(2, FollowStatus.UNFOLLOWED).copy(trackerId = 1L), mdTrack(2, FollowStatus.READING))
            coEvery { harness.getTracks.await(3L) } returns listOf(mdTrack(3, FollowStatus.UNFOLLOWED))
            every { harness.mdList.createInitialTracker(any(), any()) } returns Track.create(TrackerManager.MDLIST)
            coEvery { harness.mdList.update(any(), any()) } answers { firstArg() }
            harness.job().pushFavorites()
            coVerify(exactly = 2) { harness.mdList.update(any(), any()) }
            coVerify(exactly = 2) { harness.insertTrack.await(match { it.status == FollowStatus.READING.long }) }
            coVerify(exactly = 0) { harness.getTracks.await(4L) }
        } finally {
            mangaDexSourceIds = previous
        }
    }

    @Test
    fun initialTracksOnlyWhenMissing() = runTest {
        every { harness.mdList.createInitialTracker(any(), any()) } returns Track.create(TrackerManager.MDLIST)
        coEvery { harness.mdList.refresh(any()) } answers { firstArg() }
        coEvery { harness.getTracks.await(1L) } returns emptyList()
        coEvery { harness.getTracks.await(2L) } returns listOf(mdTrack(2, FollowStatus.READING).copy(trackerId = 1L))
        coEvery { harness.getTracks.await(3L) } returns listOf(mdTrack(3, FollowStatus.READING))
        harness.job().addInitialMdListTracks(listOf(1L, 2L, 3L).map { libraryEntry(libManga(it)) })
        coVerify(exactly = 2) { harness.insertTrack.await(any()) }
    }

    @Test
    fun initialTrackErrorsAreSkipped() = runTest {
        coEvery { harness.getTracks.await(1L) } throws IllegalStateException("db")
        coEvery { harness.getTracks.await(2L) } returns listOf(mdTrack(2, FollowStatus.READING))
        harness.job().addInitialMdListTracks(listOf(1L, 2L).map { libraryEntry(libManga(it)) })
        coVerify { harness.getTracks.await(2L) }
    }

    @Test
    fun initialTrackCancelStops() = runTest {
        coEvery { harness.getTracks.await(1L) } throws CancellationException("stop")
        shouldThrow<CancellationException> {
            harness.job().addInitialMdListTracks(listOf(libraryEntry(libManga(1))))
        }
    }

    private fun mdTrack(mangaId: Long, status: FollowStatus) = DomainTrack(
        id = mangaId,
        mangaId = mangaId,
        trackerId = TrackerManager.MDLIST,
        remoteId = 0,
        libraryId = null,
        title = "",
        lastChapterRead = 0.0,
        totalChapters = 0,
        status = status.long,
        score = 0.0,
        remoteUrl = "",
        startDate = 0,
        finishDate = 0,
        private = false,
    )

    private companion object {
        const val MD = 2_499_283_573_021_220_255L
    }
}
