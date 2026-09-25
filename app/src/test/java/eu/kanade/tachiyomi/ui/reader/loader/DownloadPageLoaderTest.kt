package eu.kanade.tachiyomi.ui.reader.loader

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.readerChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import mihon.core.common.archive.ArchiveReader
import mihon.core.common.archive.archiveReader
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class DownloadPageLoaderTest {

    private val resolver = mockk<ContentResolver>()
    private val app = mockk<Application>()
    private val downloadManager = mockk<DownloadManager>()
    private val downloadProvider = mockk<DownloadProvider>()
    private val source = mockk<Source>()
    private val manga = Manga.create().copy(id = 10L, ogTitle = "Title")
    private val chapter = readerChapter()

    @Before
    fun setUp() {
        every { app.contentResolver } returns resolver
        every { app.externalCacheDir } returns null
        startKoin { modules(module { single { app } single { ReaderPreferences(MapPreferenceStore()) } }) }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun loader() = DownloadPageLoader(
        chapter = chapter,
        manga = manga,
        source = source,
        downloadManager = downloadManager,
        downloadProvider = downloadProvider,
    )

    private fun chapterDir(dir: UniFile?) {
        every { downloadProvider.findChapterDir(any(), any(), any(), "Title", source) } returns dir
    }

    @Test
    fun directoryPagesOpenUris() = runBlocking {
        val uri = Uri.parse("file:///p0")
        every { resolver.openInputStream(uri) } answers { byteArrayOf(5).inputStream() }
        every { resolver.openInputStream(Uri.EMPTY) } answers { byteArrayOf(6).inputStream() }
        every { downloadManager.buildPageList(source, manga, any()) } returns listOf(
            Page(0, "/0", "https://0", uri),
            Page(1, "/1", "https://1"),
        )
        chapterDir(null)
        val loader = loader()
        loader.isLocal shouldBe true
        val pages = loader.getPages()
        pages.map { it.stream!!().read() } shouldBe listOf(5, 6)
        pages[1].imageUrl shouldBe "https://1"
        pages.forEach { it.status shouldBe Page.State.Ready }
        loader.loadPage(pages[0])
        loader.recycle()
        loader.isRecycled shouldBe true
        loader.isLocal = false
        loader.isLocal shouldBe false
    }

    @Test
    fun folderIsNotAnArchive() = runBlocking {
        val dir = mockk<UniFile>()
        every { dir.isFile } returns false
        chapterDir(dir)
        every { downloadManager.buildPageList(source, manga, any()) } returns emptyList()
        loader().getPages() shouldBe emptyList()
    }

    @Test
    fun archiveDelegates() = runBlocking {
        val file = mockk<UniFile>()
        every { file.isFile } returns true
        chapterDir(file)
        val reader = mockk<ArchiveReader>(relaxed = true)
        mockkStatic("mihon.core.common.archive.ArchiveReaderKt")
        every { file.archiveReader(app) } returns reader
        mockkConstructor(ArchivePageLoader::class)
        coEvery { anyConstructed<ArchivePageLoader>().getPages() } returns emptyList()
        val loader = loader()
        loader.getPages() shouldBe emptyList()
        val page = ReaderPage(0)
        loader.loadPage(page)
        coVerify { anyConstructed<ArchivePageLoader>().loadPage(page) }
        loader.recycle()
        verify { reader.close() }
    }
}
