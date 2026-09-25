package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.autoscrollInterval
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.metadata.base.raise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class ReaderViewModelInitTest {

    private val harness = ReaderVmHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
        mockkConstructor(ChapterLoader::class)
        coEvery { anyConstructed<ChapterLoader>().loadChapter(any(), any()) } returns Unit
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt")
        every { harness.sourceManager.isInitialized } returns MutableStateFlow(true)
        coEvery { harness.getManga.await(10L) } returns harness.manga
        harness.chapters(domainChapter(1L), domainChapter(2L))
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        harness.stop()
    }

    private fun chaptersOf(vm: ReaderViewModel, chapter: eu.kanade.tachiyomi.ui.reader.model.ReaderChapter) {
        vm.updateState { it.copy(viewerChapters = ViewerChapters(chapter, null, null)) }
    }

    @Test
    fun restoresSavedPage() {
        val vm = harness.viewModel()
        chaptersOf(vm, readerChapter(id = 1L))
        vm.chapterPageIndex = 3
        val second = readerChapter(id = 2L, lastPageRead = 7)
        chaptersOf(vm, second)
        awaitUntil { vm.chapterId == 2L }
        second.requestedPage shouldBe 3
    }

    @Test
    fun resumesUnreadChapters() {
        val vm = harness.viewModel()
        chaptersOf(vm, readerChapter(id = 1L))
        val unread = readerChapter(id = 2L, lastPageRead = 7)
        chaptersOf(vm, unread)
        awaitUntil { vm.chapterId == 2L }
        unread.requestedPage shouldBe 7
        val read = readerChapter(id = 3L, read = true, lastPageRead = 5)
        chaptersOf(vm, read)
        awaitUntil { vm.chapterId == 3L }
        read.requestedPage shouldBe 0
    }

    @Test
    fun autoscrollFrequencyParses() {
        val vm = harness.viewModel()
        vm.setAutoScrollFrequency("2.5")
        awaitUntil { vm.state.value.isAutoScrollEnabled }
        harness.readerPreferences.autoscrollInterval.get() shouldBe 2.5f
        for (text in listOf("abc", "0", "10000")) {
            vm.setAutoScrollFrequency("1")
            awaitUntil { vm.state.value.isAutoScrollEnabled }
            vm.setAutoScrollFrequency(text)
            awaitUntil { !vm.state.value.isAutoScrollEnabled }
            harness.readerPreferences.autoscrollInterval.get() shouldBe -1f
        }
    }

    @Test
    fun initLoadsTheChapter() = runBlocking {
        every { harness.sourceManager.getOrStub(1L) } returns mockk<HttpSource>()
        val vm = harness.viewModel()
        vm.init(mangaId = 10L, initialChapterId = 2L, page = null) shouldBe Result.success(true)
        vm.chapterId shouldBe 2L
        vm.state.value.currentChapter!!.chapter.id shouldBe 2L
        vm.state.value.meta.shouldBeNull()
        vm.state.value.ehAutoscrollFreq shouldBe "3.0"
        vm.loader shouldBe vm.loader
        vm.init(mangaId = 10L, initialChapterId = 1L, page = null) shouldBe Result.success(true)
        Unit
    }

    @Test
    fun initKeepsRestoredChapter() = runBlocking {
        every { harness.sourceManager.getOrStub(1L) } returns mockk<HttpSource>()
        harness.readerPreferences.autoscrollInterval.set(-1f)
        val vm = harness.viewModel()
        vm.chapterId = 1L
        vm.init(mangaId = 10L, initialChapterId = 2L, page = 4).isSuccess shouldBe true
        vm.chapterId shouldBe 1L
        vm.state.value.ehAutoscrollFreq shouldBe ""
        vm.state.value.isAutoScrollEnabled shouldBe false
    }

    @Test
    fun initWithoutMangaFails() = runBlocking {
        coEvery { harness.getManga.await(10L) } returns null
        harness.viewModel().init(mangaId = 10L, initialChapterId = 1L, page = null) shouldBe Result.success(false)
        coEvery { harness.getManga.await(10L) } throws IllegalStateException("db")
        harness.viewModel().init(10L, 1L, null).exceptionOrNull()!!.message shouldBe "db"
        coEvery { harness.getManga.await(10L) } throws CancellationException("stop")
        shouldThrow<CancellationException> { harness.viewModel().init(10L, 1L, null) }
        Unit
    }

    @Test
    fun mergedSourceReadsReferences() = runBlocking {
        val child = harness.manga.copy(id = 11L)
        coEvery { harness.getMergedReferencesById.await(10L) } returns emptyList()
        coEvery { harness.getMergedMangaById.await(10L) } returns listOf(child)
        val vm = harness.viewModel()
        val merged = vm.mergedDataFor(harness.manga, mockk<MergedSource>())
        merged.manga shouldBe mapOf(11L to child)
        vm.mergedDataFor(harness.manga, mockk<HttpSource>()).manga shouldBe emptyMap()
    }

    @Test
    fun metadataSourceRaisesMeta() = runBlocking {
        val source = mockk<MetadataSource<RaisedSearchMetadata, Any>>()
        every { source.metaClass } returns RaisedSearchMetadata::class
        val vm = harness.viewModel()
        val none = ChapterLoader.MergedData(emptyList(), emptyMap())
        coEvery { harness.getFlatMetadataById.await(10L) } returns null
        vm.publishInitialState(harness.manga, source, none)
        vm.state.value.meta.shouldBeNull()
        val meta = mockk<RaisedSearchMetadata>()
        val flat = mockk<FlatMetadata>()
        mockkStatic("exh.metadata.metadata.base.FlatMetadataKt")
        every { flat.raise(any<KClass<RaisedSearchMetadata>>()) } returns meta
        coEvery { harness.getFlatMetadataById.await(10L) } returns flat
        vm.publishInitialState(harness.manga, source, none)
        vm.state.value.meta shouldBe meta
    }
}
