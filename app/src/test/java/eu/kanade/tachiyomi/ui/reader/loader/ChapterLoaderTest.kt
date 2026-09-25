package eu.kanade.tachiyomi.ui.reader.loader

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.preserveReadingPosition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import mihon.core.common.archive.ArchiveReader
import mihon.core.common.archive.archiveReader
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.StubSource
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.io.Format
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class ChapterLoaderTest {

    private val harness = ChapterLoaderHarness()

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun load(source: Source, chapter: ReaderChapter = readerChapter(), page: Int? = null): ReaderChapter {
        runBlocking { harness.loader(source).loadChapter(chapter, page) }
        return chapter
    }

    @Test
    fun httpChapterLoads() {
        val chapter = load(mockk<HttpSource>(), readerChapter(lastPageRead = 4))
        chapter.pageLoader.shouldBeInstanceOf<HttpPageLoader>()
        chapter.requestedPage shouldBe 4
        (chapter.state as ReaderChapter.State.Loaded).pages.all { it.chapter == chapter } shouldBe true
    }

    @Test
    fun readyChapterIsKept() {
        val chapter = readerChapter()
        val loader = mockk<PageLoader>()
        chapter.state = ReaderChapter.State.Loaded(emptyList())
        chapter.pageLoader = loader
        load(mockk<Source>(), chapter).pageLoader shouldBe loader
        chapter.pageLoader = null
        load(mockk<HttpSource>(), chapter).pageLoader.shouldBeInstanceOf<HttpPageLoader>()
        chapter.state = ReaderChapter.State.Wait
        chapter.pageLoader = loader
        load(mockk<HttpSource>(), chapter).pageLoader.shouldBeInstanceOf<HttpPageLoader>()
    }

    @Test
    fun readChapterStartsAtRequested() {
        load(mockk<HttpSource>(), readerChapter(read = true, lastPageRead = 4)).requestedPage shouldBe 0
        load(mockk<HttpSource>(), readerChapter(read = true, lastPageRead = 4), page = 2).requestedPage shouldBe 2
        harness.readerPrefs.preserveReadingPosition.set(true)
        load(mockk<HttpSource>(), readerChapter(read = true, lastPageRead = 4)).requestedPage shouldBe 4
    }

    @Test
    fun emptyChapterFails() {
        harness.pages = emptyList()
        val chapter = readerChapter()
        shouldThrow<IOException> { load(mockk<HttpSource>(), chapter) }.message shouldBe "No pages found"
        (chapter.state as ReaderChapter.State.Error).error.shouldBeInstanceOf<IOException>()
    }

    @Test
    fun downloadedWinsOverSource() {
        harness.downloaded(true)
        load(mockk<HttpSource>()).pageLoader.shouldBeInstanceOf<DownloadPageLoader>()
    }

    @Test
    fun unknownSourcesFail() {
        val stub = StubSource(id = 7L, lang = "en", name = "Gone")
        shouldThrow<IllegalStateException> { load(stub) }.message shouldBe "Source not installed: $stub"
        shouldThrow<IllegalStateException> { load(mockk<Source>()) }.message shouldBe "Source not found"
    }

    @Test
    fun localFormatsPickLoader() {
        val local = mockk<LocalSource>()
        val file = mockk<UniFile>()
        mockkStatic("mihon.core.common.archive.ArchiveReaderKt")
        every { file.archiveReader(any()) } returns mockk<ArchiveReader>(relaxed = true)
        every { local.getFormat(any()) } returns Format.Directory(file)
        load(local).pageLoader.shouldBeInstanceOf<DirectoryPageLoader>()
        every { local.getFormat(any()) } returns Format.Archive(file)
        load(local).pageLoader.shouldBeInstanceOf<ArchivePageLoader>()
        every { local.getFormat(any()) } returns Format.Epub(file)
        load(local).pageLoader.shouldBeInstanceOf<EpubPageLoader>()
    }
}
