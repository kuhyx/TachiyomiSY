package eu.kanade.tachiyomi.data.library

import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
internal class MetadataUpdateJobTest {

    private val harness = LibraryJobHarness()
    private val source = mockk<Source>()
    private val sdk = Build.VERSION.SDK_INT

    @Before
    fun setUp() {
        harness.start()
        harness.securityPreferences.hideNotificationContent.set(true)
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        harness.stop()
    }

    private fun job() = MetadataUpdateJob(harness.context, harness.params())

    private fun library(vararg ids: Long) {
        coEvery { harness.getLibraryManga.await() } returns ids.map { libraryEntry(libManga(it, source = it)) }
    }

    @Test
    fun detailsRefreshPerSource() = runTest {
        library(1, 2, 3)
        every { harness.sourceManager.get(1L) } returns source
        every { harness.sourceManager.get(2L) } returns source
        harness.stubRemote(libManga(1, source = 1))
        harness.stubRemoteFailure(libManga(2, source = 2), IllegalStateException("gone"))
        job().doWork() shouldBe Result.success()
        coVerify(exactly = 2) {
            harness.updateMangaFromRemote(
                source = source,
                manga = any(),
                fetchDetails = true,
                fetchChapters = false,
                manualFetch = false,
                fetchWindow = any(),
                throttleFunc = any(),
            )
        }
        harness.logged.any { it.contains("gone") } shouldBe true
    }

    @Test
    fun brokenSourceLookupFails() = runTest {
        library(1)
        every { harness.sourceManager.get(1L) } returns null andThenThrows IllegalStateException("lookup")
        job().doWork() shouldBe Result.failure()
    }

    @Test
    fun cancellationCountsAsSuccess() = runTest {
        library(1)
        every { harness.sourceManager.get(1L) } returns null andThenThrows CancellationException("stop")
        job().doWork() shouldBe Result.success()
    }

    @Test
    fun foregroundTypeFollowsSdk() = runTest {
        job().getForegroundInfo().foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
        job().getForegroundInfo().foregroundServiceType shouldBe 0
    }

    @Test
    fun startNowSkipsARunningJob() {
        harness.running(tag = LibraryJobHarness.METADATA_TAG, state = WorkInfo.State.RUNNING)
        MetadataUpdateJob.startNow(harness.context) shouldBe false
    }

    @Test
    fun startNowEnqueues() {
        MetadataUpdateJob.startNow(harness.context) shouldBe true
        verify {
            harness.workManager.enqueueUniqueWork(
                LibraryJobHarness.METADATA_TAG,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun stopCancelsRunningWork() {
        val running = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf(LibraryJobHarness.METADATA_TAG))
        every { harness.workManager.getWorkInfos(any()) } returns immediateFuture(listOf(running))
        MetadataUpdateJob.stop(harness.context)
        verify { harness.workManager.cancelWorkById(running.id) }
    }
}
