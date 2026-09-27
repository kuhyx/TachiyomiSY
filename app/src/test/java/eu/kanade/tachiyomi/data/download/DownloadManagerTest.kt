package eu.kanade.tachiyomi.data.download

import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerTest : DownloadManagerTestBase() {

    private fun queued(vararg ids: Long): List<Download> {
        manager.downloadChapters(manga, ids.map { chapter(it) }, autoStart = false)
        return manager.queueState.value
    }

    @Test
    fun collaboratorsComeFromInjekt() {
        val defaults = DownloadManager(context)
        try {
            defaults.getDownloadCount() shouldBe 0
        } finally {
            defaults.downloader.scope.cancel()
        }
    }

    @Test
    fun chaptersJoinTheQueue() {
        manager.downloadChapters(manga, listOf(chapter(1)))
        manager.queueState.value.map { it.chapter.id } shouldBe listOf(1L)
        manager.isRunning shouldBe false
    }

    @Test
    fun pageListComesFromTheDirectory() {
        mockkObject(ImageUtil)
        every { ImageUtil.isImage(any(), any()) } answers {
            secondArg<(() -> InputStream)?>()?.invoke()?.close()
            firstArg<String?>()?.endsWith("xml") == false
        }
        val dir = downloaded(chapter(1))
        File(dir, "000.jpg").writeText("jpg")
        File(dir, "ComicInfo.xml").writeText("<x/>")
        File(dir, "sub").mkdirs()
        val pages = manager.buildPageList(source, manga, chapter(1))
        pages.map { File(requireNotNull(it.uri?.path)).name } shouldBe listOf("000.jpg", "001.png")
        pages.all { it.status == Page.State.Ready } shouldBe true
    }

    @Test
    fun emptyChapterHasNoPages() {
        File(root, "Source/Title/Ch 1").mkdirs()
        shouldThrow<IOException> { manager.buildPageList(source, manga, chapter(1)) }
        shouldThrow<IOException> { manager.buildPageList(source, manga, chapter(2)) }
    }

    @Test
    fun countsComeFromTheCache() {
        every { cache.getTotalDownloadCount() } returns 3
        every { cache.getDownloadCount(manga) } returns 2
        every { cache.isChapterDownloaded("Ch 1", null, "/c/1", "Title", 5L, false) } returns true
        manager.getDownloadCount() shouldBe 3
        manager.getDownloadCount(manga) shouldBe 2
        manager.isChapterDownloaded("Ch 1", null, "/c/1", "Title", 5L) shouldBe true
        verify { cache.isChapterDownloaded("Ch 1", null, "/c/1", "Title", 5L, false) }
    }

    @Test
    fun mangaFoldersOfASource() {
        manager.getMangaFolders(source) shouldBe emptyList()
        downloaded(chapter(1))
        manager.getMangaFolders(source).map { it.name } shouldBe listOf("Title")
    }

    @Test
    fun jobStateIsObserved() {
        val infos = MutableLiveData(listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf("Downloader"))))
        every { workManager.getWorkInfosForUniqueWorkLiveData("Downloader") } returns infos
        runBlocking { withTimeout(TIMEOUT) { manager.isDownloaderRunning.first() } } shouldBe true
    }

    @Test
    fun statusFlowStartsWithActive() {
        val (first, second) = queued(1, 2)
        first.transition(Download.State.DOWNLOADING)
        val seen = collectTwo(manager.statusFlow()) { tick ->
            second.transition(if (tick % 2 == 0) Download.State.DOWNLOADING else Download.State.QUEUE)
        }
        seen shouldBe listOf(first, second)
    }

    @Test
    fun progressFlowStartsWithActive() {
        val (first, second) = queued(1, 2)
        first.transition(Download.State.DOWNLOADING)
        val page = Page(0)
        second.pages = listOf(page)
        val seen = collectTwo(manager.progressFlow()) { tick -> page.progress = tick }
        seen shouldBe listOf(first, second)
    }

    // Collects two items while [change] keeps firing (after the collector subscribed, whenever that is).
    private fun collectTwo(flow: Flow<Download>, change: (Int) -> Unit): List<Download> = runBlocking {
        // Ticks outlast the progress flow's 50 ms debounce, so each change is seen.
        withTimeout(TIMEOUT) {
            val ticker = launch(Dispatchers.Default) {
                var tick = 0
                while (isActive) {
                    delay(TICK_MS)
                    change(++tick)
                }
            }
            flow.take(2).toList().also { ticker.cancel() }
        }
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val TICK_MS = 120L
    }
}
