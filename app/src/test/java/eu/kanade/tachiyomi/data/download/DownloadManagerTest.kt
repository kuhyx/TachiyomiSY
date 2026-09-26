package eu.kanade.tachiyomi.data.download

import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerTest : DownloadManagerTestBase() {

    private fun chapterFile(name: String): File =
        File(root, "Source/Title/Ch 1").apply { mkdirs() }.resolve(name).apply { writeText("x") }

    @Test
    fun pageListFromImagesInOrder() {
        mockkObject(ImageUtil)
        every { ImageUtil.isImage(any(), any()) } answers { firstArg<String?>()!!.endsWith(".png") }
        chapterFile("002.png")
        chapterFile("001.png")
        chapterFile("ComicInfo.xml")
        File(root, "Source/Title/Ch 1/sub.png").mkdirs()
        val pages = manager.buildPageList(source, manga, chapter(1L))
        pages.map { it.uri!!.lastPathSegment } shouldBe listOf("001.png", "002.png")
        pages.all { it.status == Page.State.Ready } shouldBe true
    }

    @Test
    fun missingOrEmptyChapterFails() {
        shouldThrow<IOException> { manager.buildPageList(source, manga, chapter(1L)) }
        File(root, "Source/Title/Ch 1").mkdirs()
        shouldThrow<IOException> { manager.buildPageList(source, manga, chapter(1L)) }
    }

    @Test
    fun queriesGoToTheCache() {
        every { cache.isChapterDownloaded(any(), any(), any(), any(), any(), any()) } returns true
        every { cache.getTotalDownloadCount() } returns 4
        every { cache.getDownloadCount(manga) } returns 2
        manager.isChapterDownloaded("Ch 1", null, "/c", "Title", 5L) shouldBe true
        manager.getDownloadCount() shouldBe 4
        manager.getDownloadCount(manga) shouldBe 2
    }

    @Test
    fun mangaFoldersOfASource() {
        manager.getMangaFolders(source) shouldBe emptyList()
        File(root, "Source/One").mkdirs()
        File(root, "Source/Two").mkdirs()
        manager.getMangaFolders(source).mapNotNull { it.name }.sorted() shouldBe listOf("One", "Two")
    }

    @Test
    fun downloadsAreQueued() {
        manager.downloadChapters(manga, listOf(chapter(1L)))
        manager.downloadChapters(manga, listOf(chapter(2L)), autoStart = false)
        manager.queueState.value.map { it.chapter.id } shouldBe listOf(1L, 2L)
        manager.isRunning shouldBe false
    }

    @Test
    fun statusFlowStartsWithActive() = runTest {
        manager.downloader.addAllToQueue(listOf(download(1L), download(2L)))
        val (active, idle) = manager.queueState.value
        active.transition(Download.State.DOWNLOADING)
        manager.statusFlow().first() shouldBe active
        val seen = mutableListOf<Download>()
        val job = launch { manager.statusFlow().take(2).toList(seen) }
        testScheduler.runCurrent()
        idle.transition(Download.State.ERROR)
        job.join()
        seen shouldBe listOf(active, idle)
    }

    @Test
    fun progressFlowStartsWithActive() = runTest {
        manager.downloader.addAllToQueue(listOf(download(1L), download(2L)))
        val active = manager.queueState.value.first()
        val page = Page(0)
        active.pages = listOf(page)
        active.transition(Download.State.DOWNLOADING)
        val seen = mutableListOf<Download>()
        val job = launch { manager.progressFlow().take(2).toList(seen) }
        advanceTimeBy(200)
        page.progress = 50
        advanceTimeBy(200)
        job.join()
        seen shouldBe listOf(active, active)
    }

    @Test
    fun defaultsComeFromInjekt() {
        loadKoinModules(
            module {
                single { provider.provider }
                single { cache }
            },
        )
        DownloadManager(context).queueState.value shouldBe emptyList()
    }

    @Test
    fun runningFlowFollowsWorkManager() = runTest {
        val running = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }
        every { workManager.getWorkInfosForUniqueWorkLiveData("Downloader") } returns MutableLiveData(listOf(running))
        manager.isDownloaderRunning.first() shouldBe true
        verify { workManager.getWorkInfosForUniqueWorkLiveData("Downloader") }
    }
}
