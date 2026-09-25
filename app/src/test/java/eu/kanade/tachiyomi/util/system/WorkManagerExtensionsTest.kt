package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.Futures.immediateFuture
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WorkManagerExtensionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun workManagerComesFromTheContext() {
        WorkManager.initialize(context, Configuration.Builder().build())
        context.workManager shouldBe WorkManager.getInstance(context)
    }

    @Test
    fun runningIsTrueForARunningTag() {
        val running = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }
        val queued = mockk<WorkInfo> { every { state } returns WorkInfo.State.ENQUEUED }
        val manager = mockk<WorkManager>()
        every { manager.getWorkInfosByTag("busy") } returns immediateFuture(listOf(queued, running))
        every { manager.getWorkInfosByTag("idle") } returns immediateFuture(listOf(queued))
        manager.isRunning("busy") shouldBe true
        manager.isRunning("idle") shouldBe false
    }

    @Test
    fun foregroundFailuresAreSwallowed() = runTest {
        val info = mockk<ForegroundInfo>()
        val worker = mockk<CoroutineWorker>()
        coEvery { worker.getForegroundInfo() } returns info
        coEvery { worker.setForeground(info) } returns Unit
        worker.setForegroundSafely()
        coEvery { worker.setForeground(info) } throws IllegalStateException("not allowed")
        worker.setForegroundSafely()
    }

    @Test
    fun aRealWorkerCannotGoForeground() = runTest {
        val worker = object : CoroutineWorker(context, mockk<WorkerParameters>(relaxed = true)) {
            override suspend fun doWork(): Result = Result.success()
        }
        worker.setForegroundSafely()
    }
}
