package eu.kanade.tachiyomi.ui.reader.loader

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.io.Format

@RunWith(RobolectricTestRunner::class)
internal class ChapterLoaderMergedTest {

    private val harness = ChapterLoaderHarness()
    private val merged = mockk<MergedSource>()
    private val child = Manga.create().copy(id = 10L, source = 3L, ogTitle = "Child")

    private val reference = MergedMangaReference(
        id = 1L,
        isInfoManga = false,
        getChapterUpdates = true,
        chapterSortMode = 0,
        chapterPriority = 0,
        downloadChapters = true,
        mergeId = 99L,
        mergeUrl = "",
        mangaId = 10L,
        mangaUrl = "/m",
        mangaSourceId = 3L,
    )

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun load(
        references: List<MergedMangaReference> = listOf(reference),
        manga: Map<Long, Manga> = mapOf(10L to child),
    ): ReaderChapter {
        val chapter = readerChapter()
        runBlocking { harness.loader(merged, references, manga).loadChapter(chapter) }
        return chapter
    }

    private fun childSource(source: Source?) {
        every { harness.sourceManager.get(3L) } returns source
    }

    @Test
    fun missingReferenceFails() {
        shouldThrow<IllegalStateException> { load(references = emptyList()) }.message shouldBe "Merge reference null"
        val other = reference.copy(mangaId = 11L)
        shouldThrow<IllegalStateException> { load(references = listOf(other)) }
    }

    @Test
    fun missingSourceFails() {
        childSource(null)
        shouldThrow<IllegalStateException> { load() }.message shouldBe "Source 3 was null"
    }

    @Test
    fun missingMangaFails() {
        childSource(mockk<HttpSource>())
        shouldThrow<IllegalStateException> { load(manga = emptyMap()) }.message shouldBe
            "Manga for merged chapter was null"
    }

    @Test
    fun childLoaderByKind() {
        childSource(mockk<HttpSource>())
        load().pageLoader.shouldBeInstanceOf<HttpPageLoader>()
        harness.downloaded(true)
        load().pageLoader.shouldBeInstanceOf<DownloadPageLoader>()
        harness.downloaded(false)
        val local = mockk<LocalSource>()
        every { local.getFormat(any()) } returns Format.Directory(mockk<UniFile>())
        childSource(local)
        load().pageLoader.shouldBeInstanceOf<DirectoryPageLoader>()
        childSource(mockk<Source>())
        shouldThrow<IllegalStateException> { load() }.message shouldBe "Source not found"
    }
}
