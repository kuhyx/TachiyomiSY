package eu.kanade.tachiyomi.data.download

import android.app.Application
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.NetworkState
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.networkStateFlow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.download.service.DownloadPreferences
import java.util.UUID

/** Boolean preferences emit their value once and complete, so the job's network watcher finishes. */
private class OneShotBooleans(
    private val inner: PreferenceStore = InMemoryPreferenceStore(),
) : PreferenceStore by inner {
    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        val preference = inner.getBoolean(key, defaultValue)
        return object : Preference<Boolean> by preference {
            override fun changes(): Flow<Boolean> = flowOf(preference.get())
        }
    }
}

@RunWith(RobolectricTestRunner::class)
internal class DownloadJobTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val manager = mockk<DownloadManager>(relaxed = true)
    private val preferences = DownloadPreferences(OneShotBooleans())
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val sdk = Build.VERSION.SDK_INT
    private var network = NetworkState(isConnected = true, isValidated = true, isWifi = true)
    private var starts = true

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { manager }
                    single { preferences }
                },
            )
        }
        mockkStatic("eu.kanade.tachiyomi.util.system.NetworkStateTrackerKt")
        every { any<Context>().activeNetworkState() } answers { network }
        every { any<Context>().networkStateFlow() } answers { flowOf(network) }
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt")
        every { any<DownloadManager>().downloaderStart() } answers { starts }
        every { any<DownloadManager>().downloaderStop(any()) } just runs
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        stopKoin()
        unmockkAll()
    }

    private fun job(): DownloadJob {
        val params = mockk<WorkerParameters>(relaxed = true)
        val updater = mockk<ForegroundUpdater>()
        every { updater.setForegroundAsync(any(), any(), any()) } returns immediateFuture(null)
        every { params.id } returns UUID.randomUUID()
        every { params.foregroundUpdater } returns updater
        return DownloadJob(context, params)
    }

    // The downloader "runs" for a few seconds at most, so a broken exit path fails instead of hanging the suite.
    private fun runningUntilDeadline() {
        val deadline = System.nanoTime() + RUNNING_NANOS
        every { manager.isRunning } answers { System.nanoTime() < deadline }
    }

    @Test
    fun offlineFailsAndStops() = runTest {
        network = NetworkState(isConnected = false, isValidated = false, isWifi = false)
        job().doWork() shouldBe Result.failure()
        verify { manager.downloaderStop(any()) }
    }

    @Test
    fun wifiOnlyNeedsWifi() = runTest {
        preferences.downloadOnlyOverWifi.set(true)
        network = network.copy(isWifi = false)
        job().doWork() shouldBe Result.failure()
        verify { manager.downloaderStop(any()) }
    }

    @Test
    fun refusedStartFails() = runTest {
        starts = false
        job().doWork() shouldBe Result.failure()
    }

    @Test
    fun runsUntilTheDownloaderStops() = runTest {
        preferences.downloadOnlyOverWifi.set(true)
        every { manager.isRunning } returnsMany listOf(true, true, false)
        job().doWork() shouldBe Result.success()
        verify(exactly = 0) { manager.downloaderStop(any()) }
    }

    @Test
    fun wifiIsOptional() = runTest {
        preferences.downloadOnlyOverWifi.set(false)
        network = network.copy(isWifi = false)
        every { manager.isRunning } returns false
        job().doWork() shouldBe Result.success()
    }

    @Test
    fun liveNetworkWatchStillEnds() = runTest {
        // The real network flow never completes; the worker must still end when the downloader does.
        every { any<Context>().networkStateFlow() } answers { MutableStateFlow(network) }
        every { manager.isRunning } returnsMany listOf(true, true, false)
        job().doWork() shouldBe Result.success()
        verify(exactly = 0) { manager.downloaderStop(any()) }
    }

    @Test
    fun losingTheNetworkEnds() = runTest {
        val offline = NetworkState(isConnected = false, isValidated = false, isWifi = false)
        every { any<Context>().networkStateFlow() } answers { flowOf(network, offline) }
        runningUntilDeadline()
        job().doWork() shouldBe Result.success()
        verify { manager.downloaderStop(any()) }
    }

    @Test
    fun stoppingTheWorkerEnds() = runTest {
        runningUntilDeadline()
        val worker = job()
        // WorkManager stops a worker through a restricted API; the test calls it as WorkManager would.
        val stop = ListenableWorker::class.java.getDeclaredMethod("stop", Int::class.javaPrimitiveType)
        Thread {
            Thread.sleep(STOP_DELAY_MS)
            stop.invoke(worker, WorkInfo.STOP_REASON_CANCELLED_BY_APP)
        }.start()
        worker.doWork() shouldBe Result.success()
        worker.isStopped shouldBe true
    }

    @Test
    fun foregroundTypeFollowsSdk() = runTest {
        val info = job().getForegroundInfo()
        info.notificationId shouldBe Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS
        info.foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
        job().getForegroundInfo().foregroundServiceType shouldBe 0
    }

    @Test
    fun companionTalksToWorkManager() {
        DownloadJob.start(context)
        verify { workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, any<OneTimeWorkRequest>()) }
        DownloadJob.stop(context)
        verify { workManager.cancelUniqueWork(TAG) }
        val running = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf(TAG))
        every { workManager.getWorkInfosForUniqueWork(TAG) } returns immediateFuture(listOf(running))
        DownloadJob.isRunning(context) shouldBe true
        every { workManager.getWorkInfosForUniqueWork(TAG) } returns immediateFuture(emptyList())
        DownloadJob.isRunning(context) shouldBe false
    }

    private companion object {
        const val TAG = "Downloader"
        const val STOP_DELAY_MS = 100L
        const val RUNNING_NANOS = 5_000_000_000L
    }
}
