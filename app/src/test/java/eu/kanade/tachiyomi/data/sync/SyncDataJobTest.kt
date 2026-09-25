package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker.Result
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
internal class SyncDataJobTest {

    private val harness = SyncManagerHarness()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val updater = mockk<ForegroundUpdater>()
    private var online = true

    @Before
    fun setUp() {
        harness.start()
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        mockkStatic("eu.kanade.tachiyomi.util.system.NetworkExtensionsKt")
        every { any<Context>().workManager } returns workManager
        every { any<Context>().isOnline() } answers { online }
        running(tag = SyncDataJob.TAG_MANUAL, state = WorkInfo.State.SUCCEEDED)
        every { updater.setForegroundAsync(any(), any(), any()) } returns immediateFuture(null)
        mockkConstructor(SyncManager::class)
        coEvery { anyConstructed<SyncManager>().syncData() } returns Unit
    }

    @After
    fun tearDown() = harness.stop()

    private fun running(tag: String, state: WorkInfo.State) {
        val info = WorkInfo(UUID.randomUUID(), state, setOf(tag))
        every { workManager.getWorkInfosByTag(tag) } returns immediateFuture(listOf(info))
    }

    private fun job(vararg tags: String): SyncDataJob {
        val params = mockk<WorkerParameters>(relaxed = true)
        every { params.tags } returns tags.toSet()
        every { params.id } returns UUID.randomUUID()
        every { params.foregroundUpdater } returns updater
        return SyncDataJob(harness.context, params)
    }

    @Test
    fun manualRunSyncs() = runTest {
        job("SyncDataJob", SyncDataJob.TAG_MANUAL).doWork() shouldBe Result.success()
        coVerify { anyConstructed<SyncManager>().syncData() }
    }

    @Test
    fun offlineAutoRunRetries() = runTest {
        online = false
        job("SyncDataJob:auto").doWork() shouldBe Result.retry()
        coVerify(exactly = 0) { anyConstructed<SyncManager>().syncData() }
    }

    @Test
    fun autoRunWaitsForManual() = runTest {
        running(tag = SyncDataJob.TAG_MANUAL, state = WorkInfo.State.RUNNING)
        job("SyncDataJob:auto").doWork() shouldBe Result.retry()
    }

    @Test
    fun failedAutoRunStillSucceeds() = runTest {
        every { updater.setForegroundAsync(any(), any(), any()) } throws IllegalStateException("background")
        coEvery { anyConstructed<SyncManager>().syncData() } throws IllegalStateException("remote down")
        job("SyncDataJob:auto").doWork() shouldBe Result.success()
        harness.logged.any { it.startsWith("Not allowed to set foreground job\n") } shouldBe true
    }

    @Test
    fun foregroundIsADataSync() = runTest {
        val info = job().getForegroundInfo()
        info.notificationId shouldBe Notifications.ID_RESTORE_PROGRESS
        info.foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    }

    @Test
    fun oldAndroidHasNoServiceType() = runTest {
        val sdk = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
        try {
            job().getForegroundInfo().foregroundServiceType shouldBe 0
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        }
    }
}
