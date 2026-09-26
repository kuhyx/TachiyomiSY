package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.saver.withSdk
import eu.kanade.tachiyomi.data.updater.allowNotifications
import eu.kanade.tachiyomi.util.system.NetworkState
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.networkStateFlow
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.download.service.DownloadPreferences

/** The worker that keeps the downloader alive while the network allows it. */
@RunWith(RobolectricTestRunner::class)
internal class DownloadJobTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val downloader = mockk<Downloader>(relaxed = true)
    private val manager = mockk<DownloadManager>(relaxed = true).also { every { it.downloader } returns downloader }
    private val wifiOnly = mockk<Preference<Boolean>>()
    private val offline = NetworkState(isConnected = false, isValidated = false, isWifi = false)
    private val mobile = NetworkState(isConnected = true, isValidated = true, isWifi = false)
    private val wifi = NetworkState(isConnected = true, isValidated = true, isWifi = true)

    @Before
    fun setUp() {
        context.allowNotifications()
        val prefs = mockk<DownloadPreferences> { every { downloadOnlyOverWifi } returns wifiOnly }
        startKoin { modules(module { single { manager } }, module { single { prefs } }) }
        every { wifiOnly.get() } returns false
        every { wifiOnly.changes() } returns flowOf(false)
        every { downloader.start() } returns true
        mockkStatic("eu.kanade.tachiyomi.util.system.NetworkStateTrackerKt")
        every { any<Context>().activeNetworkState() } returns mobile
        every { any<Context>().networkStateFlow() } returns flowOf(mobile)
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        coEvery { any<CoroutineWorker>().setForegroundSafely() } returns Unit
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    private fun job() = DownloadJob(context, mockk<WorkerParameters>(relaxed = true))

    private fun text(id: Int) = context.getString(id)

    @Test
    fun offlineFails() = runTest {
        every { any<Context>().activeNetworkState() } returns offline
        job().doWork() shouldBe ListenableWorker.Result.failure()
        verify { downloader.stop(text(R.string.download_notifier_no_network)) }
        verify(exactly = 0) { downloader.start() }
    }

    @Test
    fun wifiOnlyOnMobileFails() = runTest {
        every { wifiOnly.get() } returns true
        job().doWork() shouldBe ListenableWorker.Result.failure()
        verify { downloader.stop(text(R.string.download_notifier_text_only_wifi)) }
    }

    @Test
    fun downloaderNotStartingFails() = runTest {
        every { downloader.start() } returns false
        job().doWork() shouldBe ListenableWorker.Result.failure()
    }

    @Test
    fun runsUntilDownloaderStops() = runTest {
        every { wifiOnly.changes() } returns flowOf(true)
        every { any<Context>().networkStateFlow() } returns flowOf(wifi)
        every { manager.isRunning } returnsMany listOf(true, true, false)
        job().doWork() shouldBe ListenableWorker.Result.success()
        verify(exactly = 3) { manager.isRunning }
    }

    @Test
    fun lostNetworkEndsTheLoop() = runTest {
        every { any<Context>().networkStateFlow() } returns flowOf(offline)
        every { manager.isRunning } returns true
        job().doWork() shouldBe ListenableWorker.Result.success()
    }

    @Test
    fun stoppedWorkerEndsTheLoop() = runTest {
        val worker = spyk(job())
        every { worker.isStopped } returns true
        worker.doWork() shouldBe ListenableWorker.Result.success()
        verify(exactly = 0) { manager.isRunning }
    }

    @Test
    fun foregroundTypeFollowsSdk() = runTest {
        job().getForegroundInfo().foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        val old = withSdk(Build.VERSION_CODES.P) { runBlocking { job().getForegroundInfo() } }
        old.foregroundServiceType shouldBe 0
    }

    @Test
    fun onlyRunningWorkCounts() {
        val workManager = mockk<WorkManager>()
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        val running = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }
        val queued = mockk<WorkInfo> { every { state } returns WorkInfo.State.ENQUEUED }
        every { workManager.getWorkInfosForUniqueWork("Downloader") } returns immediateFuture(listOf(queued, running))
        DownloadJob.isRunning(context) shouldBe true
    }
}
