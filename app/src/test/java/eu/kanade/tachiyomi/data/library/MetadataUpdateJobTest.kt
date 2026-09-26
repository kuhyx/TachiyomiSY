package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.saver.withSdk
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mihon.domain.source.models.RemoteMangaUpdate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/** Refreshing every library entry's details, and starting or stopping that work. */
@RunWith(RobolectricTestRunner::class)
internal class MetadataUpdateJobTest : LibraryUpdateTestBase() {

    private val workManager = mockk<WorkManager>(relaxed = true)

    @Before
    fun setUpWorker() {
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        coEvery { any<CoroutineWorker>().setForegroundSafely() } returns Unit
        every { any<Context>().workManager } returns workManager
        every { workManager.isRunning(any()) } returns false
    }

    private fun worker() = MetadataUpdateJob(context, params())

    @Test
    fun detailsAreRefetched() = runTest {
        val one = manga(1L)
        val broken = manga(2L)
        every { sourceManager.get(9L) } returns null
        coEvery { getLibraryManga.await() } returns listOf(one, broken, manga(3L, source = 9L)).map { libraryManga(it) }
        coEvery { updateFromRemote(any<Source>(), one, any(), any(), any(), any(), any()) } returns
            Result.success(RemoteMangaUpdate(one, emptyList()))
        coEvery { updateFromRemote(any<Source>(), broken, any(), any(), any(), any(), any()) } returns
            Result.failure(IllegalStateException("offline"))
        worker().doWork() shouldBe ListenableWorker.Result.success()
        coVerify(exactly = 2) { updateFromRemote(any<Source>(), any(), true, any(), any(), any(), any()) }
        shown(Notifications.ID_LIBRARY_PROGRESS) shouldBe null
    }

    @Test
    fun failuresAndCancellation() = runTest {
        coEvery { getLibraryManga.await() } returns listOf(libraryManga(manga(1L)))
        // The first lookup is the queue-size warning, outside the worker's error handling.
        every { sourceManager.get(1L) } returns null andThenThrows IllegalStateException("no sources")
        worker().doWork() shouldBe ListenableWorker.Result.failure()
        every { sourceManager.get(1L) } returns null andThenThrows CancellationException("stopped")
        worker().doWork() shouldBe ListenableWorker.Result.success()
    }

    @Test
    fun foregroundTypeFollowsSdk() = runTest {
        worker().getForegroundInfo().foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        withSdk(Build.VERSION_CODES.P) { runBlocking { worker().getForegroundInfo() } }.foregroundServiceType shouldBe 0
    }

    @Test
    fun startsUnlessRunning() {
        MetadataUpdateJob.startNow(context) shouldBe true
        verify { workManager.enqueueUniqueWork("MetadataUpdate", any(), any<OneTimeWorkRequest>()) }
        every { workManager.isRunning("MetadataUpdate") } returns true
        MetadataUpdateJob.startNow(context) shouldBe false
    }

    @Test
    fun stopCancelsRunningWork() {
        val runId = UUID.randomUUID()
        val running = mockk<WorkInfo> { every { id } returns runId }
        every { workManager.getWorkInfos(any()) } returns immediateFuture(listOf(running))
        MetadataUpdateJob.stop(context)
        verify { workManager.cancelWorkById(runId) }
    }
}
