package eu.kanade.tachiyomi.data.library

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.io.IOException
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateReportTest {

    private val harness = LibraryJobHarness()
    private val source = mockk<Source>()
    private val notifications get() = shadowOf(harness.context.getSystemService(NotificationManager::class.java))

    @Before
    fun setUp() {
        harness.start()
        harness.securityPreferences.hideNotificationContent.set(true)
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt")
        mockkStatic("eu.kanade.tachiyomi.util.storage.FileExtensionsKt")
        every { any<DownloadManager>().startDownloads() } just runs
        every { any<File>().getUriCompat(any()) } returns Uri.EMPTY
        every { source.toString() } returns "Src"
        every { harness.sourceManager.getOrStub(any()) } returns source
    }

    @After
    fun tearDown() = harness.stop()

    private fun runWith(downloads: Boolean, failed: Boolean) = LibraryUpdateRun(0L to 0L).apply {
        newUpdates.add(libManga(1) to arrayOf(libChapter(1)))
        hasDownloads.store(downloads)
        if (failed) failedUpdates.add(libManga(2) to "boom")
    }

    @Test
    fun downloadsStartAfterNewChapters() {
        harness.job().reportRun(runWith(downloads = true, failed = false))
        verify { harness.downloadManager.startDownloads() }
        notifications.getNotification(Notifications.ID_NEW_CHAPTERS) shouldNotBe null
        notifications.getNotification(Notifications.ID_LIBRARY_ERROR) shouldBe null
    }

    @Test
    fun failuresPostTheErrorLog() {
        harness.job().reportRun(runWith(downloads = false, failed = true))
        verify(exactly = 0) { harness.downloadManager.startDownloads() }
        notifications.getNotification(Notifications.ID_LIBRARY_ERROR) shouldNotBe null
    }

    @Test
    fun emptyRunPostsNothing() {
        harness.job().reportRun(LibraryUpdateRun(0L to 0L))
        notifications.allNotifications shouldBe emptyList()
    }

    @Test
    fun errorFileGroupsBySource() {
        val errors = listOf(libManga(1, title = "A") to "boom", libManga(2, title = "B") to "boom", libManga(3) to null)
        val file = harness.job().writeErrorFile(errors)
        file.readText().lines() shouldBe listOf(
            HELP, "", "", "! boom", "  # Src", "    - A", "    - B", "", "! null", "  # Src", "    - m3", "",
        )
    }

    @Test
    fun brokenReportGivesEmptyPath() {
        every { harness.sourceManager.getOrStub(any()) } throws IllegalStateException("no source")
        harness.job().writeErrorFile(listOf(libManga(1) to "boom")).path shouldBe ""
    }

    @Test
    fun errorFileNeedsErrors() {
        harness.job().writeErrorFile(emptyList()).path shouldBe ""
    }

    @Test
    fun unwritableCacheGivesEmptyPath() {
        mockkStatic("eu.kanade.tachiyomi.util.system.ContextExtensionsKt")
        every { any<Context>().createFileInCacheDir(any()) } throws IOException("full")
        harness.job().writeErrorFile(listOf(libManga(1) to "boom")).path shouldBe ""
    }

    @Test
    fun coverRefreshNeedsASource() = runTest {
        val manga = libManga(1)
        harness.job().refreshCover(manga)
        coVerify(exactly = 0) { harness.updateMangaFromRemote(source = any(), manga = any()) }
        every { harness.sourceManager.get(manga.source) } returns source
        harness.stubRemote(manga)
        harness.job().refreshCover(manga)
        coVerify {
            harness.updateMangaFromRemote(
                source = source,
                manga = manga,
                fetchDetails = true,
                fetchChapters = false,
                manualFetch = true,
                fetchWindow = any(),
                throttleFunc = any(),
            )
        }
    }

    @Test
    fun coverFailureIsLogged() = runTest {
        val manga = libManga(1)
        every { harness.sourceManager.get(manga.source) } returns source
        harness.stubRemoteFailure(manga, IllegalStateException("cover down"))
        harness.job().refreshCover(manga)
        harness.logged.any { it.contains("cover down") } shouldBe true
    }

    private companion object {
        const val HELP = "For help on how to fix library update errors, see " +
            "https://mihon.app/docs/guides/troubleshooting/"
    }
}
