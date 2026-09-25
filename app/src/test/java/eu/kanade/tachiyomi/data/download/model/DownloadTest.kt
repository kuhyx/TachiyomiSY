package eu.kanade.tachiyomi.data.download.model

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

internal class DownloadTest {

    private val source: HttpSource = mockk()
    private val manga = Manga.create().copy(id = 1L, source = 9L)
    private val chapter = Chapter.create().copy(id = 2L, mangaId = 1L)

    private fun pages(vararg progress: Int): List<Page> = progress.mapIndexed { index, value ->
        Page(index).apply {
            this.progress = value
            if (value == 100) status = Page.State.Ready
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun withoutPagesEverythingIsZero() {
        val download = Download(source, manga, chapter)
        download.totalProgress shouldBe 0
        download.downloadedImages shouldBe 0
        download.progress shouldBe 0
        download.status shouldBe Download.State.NOT_DOWNLOADED
    }

    @Test
    fun pagesDriveTheProgress() {
        val download = Download(source, manga, chapter)
        download.pages = pages(100, 50)
        download.totalProgress shouldBe 150
        download.downloadedImages shouldBe 1
        download.progress shouldBe 75
    }

    @Test
    fun transitionUpdatesTheFlow() = runTest {
        val download = Download(source, manga, chapter)
        download.transition(Download.State.QUEUE)
        download.statusFlow.first() shouldBe Download.State.QUEUE
        Download.State.entries.map { it.value } shouldBe listOf(0, 1, 2, 3, 4)
    }

    @Test
    fun progressWaitsForPages() = runTest {
        val download = Download(source, manga, chapter)
        val seen = mutableListOf<Int>()
        val job = launch { download.progressFlow.take(2).toList(seen) }
        advanceTimeBy(200)
        download.pages = pages(40, 60)
        advanceTimeBy(200)
        job.join()
        seen shouldBe listOf(0, 50)
    }

    @Test
    fun knownPagesStartTheFlowAtOnce() = runTest {
        val download = Download(source, manga, chapter)
        download.pages = pages(100, 100)
        download.progressFlow.first() shouldBe 100
    }

    @Test
    fun equalityIsByContent() {
        val one = Download(source, manga, chapter)
        one shouldBe one
        one shouldBe Download(source, manga, chapter)
        one.hashCode() shouldBe Download(source, manga, chapter).hashCode()
        one shouldNotBe Download(mockk(), manga, chapter)
        one shouldNotBe Download(source, manga.copy(id = 5L), chapter)
        one shouldNotBe Download(source, manga, chapter.copy(id = 7L))
        one.equals("other") shouldBe false
    }

    @Test
    fun fromChapterIdResolves() = runTest {
        val getChapter = mockk<GetChapter>()
        val getManga = mockk<GetManga>()
        val sourceManager = mockk<SourceManager>()
        coEvery { getChapter.await(2L) } returns chapter
        coEvery { getChapter.await(3L) } returns null
        coEvery { getChapter.await(4L) } returns chapter.copy(id = 4L, mangaId = 8L)
        coEvery { getManga.await(1L) } returns manga
        coEvery { getManga.await(8L) } returns manga.copy(id = 8L, source = 10L)
        every { sourceManager.get(9L) } returns source
        every { sourceManager.get(10L) } returns mockk<Source>()
        startKoin {
            modules(
                module {
                    single { getChapter }
                    single { getManga }
                    single { sourceManager }
                },
            )
        }
        Download.fromChapterId(2L) shouldBe Download(source, manga, chapter)
        Download.fromChapterId(3L).shouldBeNull()
        Download.fromChapterId(4L).shouldBeNull()
        coEvery { getManga.await(1L) } returns null
        Download.fromChapterId(2L).shouldBeNull()
    }
}
