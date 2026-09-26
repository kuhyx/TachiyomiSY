package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.saver.plainFileUris
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Cover refreshes, the end-of-run report and the error file. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateRunTest : LibraryUpdateTestBase() {

    private val errorFile: File get() = context.externalCacheDir!!.resolve("mihon_update_errors.txt")

    @Test
    fun coversAreRefetched() = runTest {
        val one = manga(1L)
        val broken = manga(2L)
        every { sourceManager.get(9L) } returns null
        coEvery { updateFromRemote(any<Source>(), one, any(), any(), any(), any(), any()) } returns
            Result.success(RemoteMangaUpdate(one, emptyList()))
        coEvery { updateFromRemote(any<Source>(), broken, any(), any(), any(), any(), any()) } returns
            Result.failure(IllegalStateException("offline"))
        job().apply {
            mangaToUpdate = listOf(one, broken, manga(3L, source = 9L)).map { libraryManga(it) }
            updateCovers()
        }
        coVerify { updateFromRemote(any<Source>(), one, true, false, true, any(), any()) }
        coVerify(exactly = 2) { updateFromRemote(any<Source>(), any(), any(), any(), any(), any(), any()) }
        shown(Notifications.ID_LIBRARY_PROGRESS) shouldBe null
    }

    @Test
    fun emptyRunReportsNothing() {
        job().reportRun(LibraryUpdateRun(0L to 0L))
        shown(Notifications.ID_NEW_CHAPTERS) shouldBe null
        shown(Notifications.ID_LIBRARY_ERROR) shouldBe null
    }

    @Test
    fun failuresAloneAreReported() {
        plainFileUris()
        val run = LibraryUpdateRun(0L to 0L).apply { failedUpdates.add(manga(1L) to null) }
        job().reportRun(run)
        shown(Notifications.ID_NEW_CHAPTERS) shouldBe null
        shown(Notifications.ID_LIBRARY_ERROR) shouldNotBe null
        errorFile.readText().contains("! null") shouldBe true
    }

    @Test
    fun noErrorsWriteNoFile() {
        job().writeErrorFile(emptyList()) shouldBe File("")
    }

    @Test
    fun unwritableErrorFileIsEmptyPath() {
        errorFile.mkdirs()
        errorFile.resolve("keep").writeText("x")
        job().writeErrorFile(listOf(manga(1L) to "boom")) shouldBe File("")
    }

    @Test
    fun failedReportIsEmptyPath() {
        every { sourceManager.getOrStub(any()) } throws IllegalStateException("no sources")
        job().writeErrorFile(listOf(manga(1L) to "boom")) shouldBe File("")
    }
}
