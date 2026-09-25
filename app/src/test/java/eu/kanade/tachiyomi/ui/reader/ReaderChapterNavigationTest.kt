package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ReaderChapterNavigationTest {

    private val harness = ReaderVmHarness()
    private val loader = mockk<ChapterLoader>(relaxed = true)

    @BeforeEach
    fun setUp() {
        harness.start()
        harness.chapters(domainChapter(1L), domainChapter(2L), domainChapter(3L))
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun readerVm(withLoader: Boolean = true): ReaderViewModel = harness.loadedViewModel(chapterId = 2L).also {
        if (withLoader) it.loader = loader
    }

    @Test
    fun loadChapterSetsNeighbours() {
        runBlocking {
            val vm = readerVm()
            val first = vm.chapterList[0]
            vm.loadChapter(loader, first).prevChapter.shouldBeNull()
            vm.state.value.viewerChapters!!.nextChapter shouldBe vm.chapterList[1]
            val chapters = vm.loadChapter(loader, vm.chapterList[1], page = 3)
            chapters.prevChapter shouldBe first
            chapters.nextChapter shouldBe vm.chapterList[2]
            vm.state.value.bookmarked shouldBe false
            coVerify { loader.loadChapter(vm.chapterList[1], 3) }
        }
    }

    @Test
    fun newChapterNeedsLoader() {
        val vm = readerVm(withLoader = false)
        vm.loadNewChapter(vm.chapterList[0])
        vm.state.value.viewerChapters.shouldBeNull()
        val loaded = readerVm()
        loaded.loadNewChapter(loaded.chapterList[0])
        awaitUntil { loaded.state.value.currentChapter == loaded.chapterList[0] }
    }

    @Test
    fun newChapterSwallowsErrors() {
        val vm = readerVm()
        coEvery { loader.loadChapter(any(), any()) } throws IllegalStateException("bad")
        vm.loadNewChapter(vm.chapterList[0])
        coVerify(timeout = 5_000) { loader.loadChapter(vm.chapterList[0], null) }
        coEvery { loader.loadChapter(any(), any()) } throws CancellationException("stop")
        vm.loadNewChapter(vm.chapterList[1])
        coVerify(timeout = 5_000) { loader.loadChapter(vm.chapterList[1], null) }
        vm.state.value.viewerChapters.shouldBeNull()
    }

    @Test
    fun dialogPicksListedChapter() {
        val vm = readerVm()
        vm.loadNewChapterFromDialog(domainChapter(9L))
        vm.loadNewChapterFromDialog(domainChapter(3L))
        awaitUntil { vm.state.value.currentChapter == vm.chapterList[2] }
    }

    @Test
    fun adjacentLoadsAndRecovers() {
        runBlocking {
            readerVm(withLoader = false).loadAdjacent(readerChapter())
            val vm = readerVm()
            vm.loadNextChapter()
            vm.loadPreviousChapter()
            vm.loadAdjacent(vm.chapterList[1])
            vm.loadNextChapter()
            vm.state.value.currentChapter shouldBe vm.chapterList[2]
            vm.loadPreviousChapter()
            vm.state.value.currentChapter shouldBe vm.chapterList[1]
            coEvery { loader.loadChapter(any(), any()) } throws IllegalStateException("bad")
            vm.loadAdjacent(vm.chapterList[0])
            vm.state.value.isLoadingAdjacentChapter shouldBe false
            coEvery { loader.loadChapter(any(), any()) } throws CancellationException("stop")
            shouldThrow<CancellationException> { vm.loadAdjacent(vm.chapterList[0]) }
        }
    }

    @Test
    fun preloadSkipsLoadedChapters() {
        runBlocking {
            val vm = readerVm()
            val loaded = readerChapter().also { it.state = ReaderChapter.State.Loaded(emptyList()) }
            val loading = readerChapter().also { it.state = ReaderChapter.State.Loading }
            vm.preload(loaded)
            vm.preload(loading)
            readerVm(withLoader = false).preload(readerChapter())
            coVerify(exactly = 0) { loader.loadChapter(any(), any()) }
        }
    }

    @Test
    fun preloadRechecksOnlineChapters() {
        runBlocking {
            val online = mockk<PageLoader>()
            every { online.isLocal } returns false
            val chapter = readerChapter().also {
                it.pageLoader = online
                it.state = ReaderChapter.State.Error(IllegalStateException("x"))
            }
            val noManga = harness.viewModel().also { it.loader = loader }
            noManga.preload(chapter)
            every {
                harness.downloadManager.isChapterDownloaded(
                    chapterName = any(),
                    chapterScanlator = any(),
                    chapterUrl = any(),
                    mangaTitle = any(),
                    sourceId = any(),
                    skipCache = true,
                )
            } returns true
            readerVm().preload(chapter)
            chapter.state shouldBe ReaderChapter.State.Wait
            every { online.isLocal } returns true
            readerVm().preload(chapter)
            coVerify(exactly = 2) { loader.loadChapter(chapter, null) }
        }
    }

    @Test
    fun preloadAnnouncesReload() {
        runBlocking {
            val vm = readerVm()
            val event = async(start = CoroutineStart.UNDISPATCHED) { vm.eventFlow.first() }
            val online = mockk<PageLoader>()
            every { online.isLocal } returns false
            vm.preload(readerChapter().also { it.pageLoader = online })
            event.await() shouldBe ReaderViewModel.Event.ReloadViewerChapters
            coEvery { loader.loadChapter(any(), any()) } throws IllegalStateException("bad")
            vm.preload(readerChapter())
            coEvery { loader.loadChapter(any(), any()) } throws CancellationException("stop")
            shouldThrow<CancellationException> { vm.preload(readerChapter()) }
        }
    }

    @Test
    fun adjacentOnIoDoesNotSuspend() {
        runBlocking(Dispatchers.IO) {
            val vm = readerVm()
            vm.loadAdjacent(vm.chapterList[1])
            vm.loadNextChapter()
            vm.loadPreviousChapter()
            vm.state.value.currentChapter shouldBe vm.chapterList[1]
        }
    }

    @Test
    fun loadReplacesOldChapters() {
        runBlocking {
            val vm = readerVm()
            val old = readerChapter(id = 7L).apply { ref() }
            vm.updateState { it.copy(viewerChapters = ViewerChapters(old, null, null)) }
            vm.loadChapter(loader, vm.chapterList[0])
            old.state shouldBe ReaderChapter.State.Wait
        }
    }
}
