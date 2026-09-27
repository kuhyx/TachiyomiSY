package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.source.Source
import exh.source.EH_SOURCE_ID
import exh.source.mangaDexSourceIds
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateChaptersTest {

    private val harness = LibraryJobHarness()
    private val source = mockk<Source>()
    private val previousMangaDex = mangaDexSourceIds

    @Before
    fun setUp() {
        harness.start()
        mangaDexSourceIds = listOf(MD_SOURCE)
        harness.securityPreferences.hideNotificationContent.set(true)
        every { harness.sourceManager.getOrStub(any()) } returns source
        every { harness.sourceManager.get(any()) } returns source
        coEvery { harness.getManga.await(any<Long>()) } answers { libManga(firstArg()) }
        coEvery { harness.getTracks.await(any<Long>()) } returns emptyList()
        every { harness.mdList.createInitialTracker(any(), any()) } returns Track.create(MD_SOURCE)
        coEvery { harness.mdList.refresh(any()) } answers { firstArg() }
    }

    @After
    fun tearDown() {
        mangaDexSourceIds = previousMangaDex
        harness.stop()
    }

    private fun job(vararg manga: Long, source: Long = 1L) = harness.job().apply {
        mangaToUpdate = manga.map { libraryEntry(libManga(it, source = source)) }
        manga.forEach { harness.stubRemote(libManga(it, source = source)) }
    }

    @Test
    fun everySourceIsUpdated() = runTest {
        job(1, 2).updateChapterList()
        coVerify(exactly = 2) {
            harness.updateMangaFromRemote(
                source = source,
                manga = any(),
                fetchDetails = false,
                fetchChapters = true,
                manualFetch = false,
                fetchWindow = any(),
                throttleFunc = any(),
            )
        }
    }

    @Test
    fun excludedSourcesAreSkipped() = runTest {
        job(1, source = EH_SOURCE_ID).updateChapterList()
        coVerify(exactly = 0) { harness.getManga.await(any<Long>()) }
    }

    @Test
    fun loggedMdListGetsTracks() = runTest {
        every { harness.mdList.isLoggedIn } returns true
        job(1, source = MD_SOURCE).updateChapterList()
        coVerify { harness.insertTrack.await(any()) }
    }

    @Test
    fun otherSourcesGetNoMdTracks() = runTest {
        every { harness.mdList.isLoggedIn } returns true
        job(1).updateChapterList()
        every { harness.mdList.isLoggedIn } returns false
        job(1, source = MD_SOURCE).updateChapterList()
        coVerify(exactly = 0) { harness.insertTrack.await(any()) }
    }

    @Test
    fun coversRefreshEachEntry() = runTest {
        job(1, 2).updateCovers()
        coVerify(exactly = 2) {
            harness.updateMangaFromRemote(
                source = source,
                manga = any(),
                fetchDetails = true,
                fetchChapters = false,
                manualFetch = true,
                fetchWindow = any(),
                throttleFunc = any(),
            )
        }
    }

    private companion object {
        const val MD_SOURCE = 2_499_283_573_021_220_255L
    }
}
