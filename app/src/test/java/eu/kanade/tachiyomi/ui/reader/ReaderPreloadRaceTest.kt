package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** A chapter another caller finished loading while its download state was being checked is not loaded again. */
internal class ReaderPreloadRaceTest {
    private val harness = ReaderVmHarness()
    private val loader = mockk<ChapterLoader>(relaxed = true)

    @BeforeEach
    fun setUp() = harness.start()

    @AfterEach
    fun tearDown() = harness.stop()

    @Test
    fun loadedMeanwhileIsSkipped() {
        val online = mockk<PageLoader> { every { isLocal } returns false }
        val chapter = readerChapter().also { it.pageLoader = online }
        every {
            harness.downloadManager.isChapterDownloaded(
                chapterName = any(),
                chapterScanlator = any(),
                chapterUrl = any(),
                mangaTitle = any(),
                sourceId = any(),
                skipCache = true,
            )
        } answers {
            chapter.state = ReaderChapter.State.Loading
            false
        }
        val vm = harness.loadedViewModel().also { it.loader = loader }
        runBlocking { vm.preload(chapter) }
        coVerify(exactly = 0) { loader.loadChapter(any(), any()) }
    }
}
