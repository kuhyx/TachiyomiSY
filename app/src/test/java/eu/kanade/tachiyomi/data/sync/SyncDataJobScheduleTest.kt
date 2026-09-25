package eu.kanade.tachiyomi.data.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class SyncDataJobScheduleTest {

    private val harness = SyncManagerHarness()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val context get() = harness.context

    @Before
    fun setUp() {
        harness.start()
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { any<Context>().workManager } returns workManager
        runningJobs(WorkInfo.State.SUCCEEDED)
    }

    @After
    fun tearDown() = harness.stop()

    private fun runningJobs(state: WorkInfo.State) {
        val info = WorkInfo(UUID.randomUUID(), state, setOf("SyncDataJob"))
        every { workManager.getWorkInfosByTag("SyncDataJob") } returns immediateFuture(listOf(info))
    }

    @Test
    fun runningFollowsTheJobTag() {
        SyncDataJob.isRunning(context) shouldBe false
        runningJobs(WorkInfo.State.RUNNING)
        SyncDataJob.isRunning(context) shouldBe true
    }

    @Test
    fun positiveIntervalSchedules() {
        val request = slot<PeriodicWorkRequest>()
        SyncDataJob.setupTask(context, prefInterval = 30)
        verify {
            workManager.enqueueUniquePeriodicWork(
                "SyncDataJob:auto",
                ExistingPeriodicWorkPolicy.UPDATE,
                capture(request),
            )
        }
        request.captured.workSpec.intervalDuration shouldBe TimeUnit.MINUTES.toMillis(30)
        request.captured.tags.containsAll(listOf("SyncDataJob", "SyncDataJob:auto")) shouldBe true
    }

    @Test
    fun intervalComesFromPreferences() {
        harness.preferences.syncInterval.set(0)
        SyncDataJob.setupTask(context)
        verify { workManager.cancelUniqueWork("SyncDataJob:auto") }
        harness.preferences.syncInterval.set(60)
        SyncDataJob.setupTask(context)
        verify { workManager.enqueueUniquePeriodicWork("SyncDataJob:auto", any(), any()) }
    }

    @Test
    fun startNowPicksTheTag() {
        SyncDataJob.startNow(context, manual = true)
        verify {
            workManager.enqueueUniqueWork(SyncDataJob.TAG_MANUAL, ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>())
        }
        SyncDataJob.startNow(context)
        verify {
            workManager.enqueueUniqueWork("SyncDataJob:auto", ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun startNowSkipsARunningJob() {
        runningJobs(WorkInfo.State.RUNNING)
        SyncDataJob.startNow(context, manual = true)
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun stopCancelsAndReschedulesAuto() {
        val auto = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf("SyncDataJob", "SyncDataJob:auto"))
        val manual = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf("SyncDataJob", SyncDataJob.TAG_MANUAL))
        every { workManager.getWorkInfos(any()) } returns immediateFuture(listOf(auto, manual))
        SyncDataJob.stop(context)
        verify { workManager.cancelWorkById(auto.id) }
        verify { workManager.cancelWorkById(manual.id) }
        verify(exactly = 1) { workManager.cancelUniqueWork("SyncDataJob:auto") }
    }
}
