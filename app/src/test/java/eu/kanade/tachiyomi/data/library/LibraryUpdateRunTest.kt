package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.source.Source
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.source.model.SourceNotInstalledException
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateRunTest {

    private val harness = LibraryJobHarness()
    private val manga = libManga(id = 1)
    private val chapters = listOf(libChapter(id = 1), libChapter(id = 2))

    @Before
    fun setUp() {
        harness.start()
        every { harness.sourceManager.getOrStub(any()) } returns mockk<Source>()
        coEvery { harness.getManga.await(manga.id) } returns manga
    }

    @After
    fun tearDown() = harness.stop()

    private fun run() = LibraryUpdateRun(0L to 0L)

    @Test
    fun skipsWhenNotInLibrary() = runTest {
        coEvery { harness.getManga.await(manga.id) } returns null
        harness.job().updateIfInLibrary(manga, run())
        coEvery { harness.getManga.await(manga.id) } returns manga.copy(favorite = false)
        harness.job().updateIfInLibrary(manga, run())
        coVerify(exactly = 0) { harness.updateMangaFromRemote(source = any(), manga = any()) }
    }

    @Test
    fun newChaptersAreDownloaded() = runTest {
        harness.stubRemote(manga, chapters)
        coEvery { harness.filterChaptersForDownload.await(manga, any()) } returns chapters
        val run = run()
        harness.job().updateIfInLibrary(manga, run)
        verify { harness.downloadManager.downloadChapters(manga, chapters, false) }
        run.hasDownloads.load() shouldBe true
        run.newUpdates.single().second.map { it.id } shouldBe listOf(2L, 1L)
        run.progressCount.load() shouldBe 1
        harness.libraryPreferences.newUpdatesCount.get() shouldBe 2
    }

    @Test
    fun filteredOutChaptersStayQueued() = runTest {
        harness.stubRemote(manga, chapters)
        coEvery { harness.filterChaptersForDownload.await(manga, any()) } returns emptyList()
        val run = run()
        harness.job().updateIfInLibrary(manga, run)
        run.hasDownloads.load() shouldBe false
        run.newUpdates.size shouldBe 1
        verify(exactly = 0) { harness.downloadManager.downloadChapters(any(), any(), any()) }
    }

    @Test
    fun nothingNewRecordsNothing() = runTest {
        harness.stubRemote(manga, chapters, favorite = false)
        val run = run()
        harness.job().updateIfInLibrary(manga, run)
        harness.stubRemote(manga, emptyList())
        harness.job().updateIfInLibrary(manga, run)
        run.newUpdates.isEmpty() shouldBe true
        run.failedUpdates.isEmpty() shouldBe true
    }

    @Test
    fun failuresKeepTheirMessage() = runTest {
        val run = run()
        listOf(NoChaptersException(), SourceNotInstalledException(), IllegalStateException("boom")).forEach {
            harness.stubRemoteFailure(manga, it)
            harness.job().updateIfInLibrary(manga, run)
        }
        run.failedUpdates.map { it.second } shouldBe listOf("No chapters found", "Source not found", "boom")
    }

    @Test
    fun mergedDownloadsPerChild() = runTest {
        val merged = libManga(id = 9, source = MERGED_SOURCE_ID)
        val child = libManga(id = 5)
        coEvery { harness.getMergedMangaForDownloading.await(merged.id) } returns listOf(child)
        val own = libChapter(id = 1, mangaId = 5)
        val orphan = libChapter(id = 2, mangaId = 7)
        harness.job().downloadChapters(merged, listOf(own, orphan))
        verify { harness.downloadManager.downloadChapters(child, listOf(own), false) }
        verify(exactly = 1) { harness.downloadManager.downloadChapters(any(), any(), any()) }
    }

    @Test
    fun metadataPrefDrivesDetails() = runTest {
        harness.libraryPreferences.autoUpdateMetadata.set(true)
        harness.stubRemote(manga, chapters)
        harness.job().updateManga(manga, 1L to 2L) shouldBe chapters
        coVerify {
            harness.updateMangaFromRemote(
                source = any(),
                manga = manga,
                fetchDetails = true,
                fetchChapters = true,
                manualFetch = false,
                fetchWindow = 1L to 2L,
                throttleFunc = any(),
            )
        }
    }

    @Test
    fun runTalliesAreAValue() {
        val run = run()
        run shouldBe run.copy()
        run.hashCode() shouldBe LibraryUpdateRun(0L to 0L).hashCode()
        run.toString() shouldBe "LibraryUpdateRun(fetchWindow=(0, 0))"
        run.component1() shouldBe (0L to 0L)
    }
}
